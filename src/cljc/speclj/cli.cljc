(ns speclj.cli
  (:require #?(:clj [trptcolin.versioneer.core :as version])
            #?@(:cljs    []
                :default [[clojure.java.io :as jio]])
            [speclj.args :as args]
            [clojure.set :as set]
            [speclj.config :as config]
            [speclj.line-filter :as line-filter]
            [speclj.platform :refer [endl print-stack-trace]]
            [speclj.reporting :refer [report-message* stack-trace-str]]
            [speclj.run.standard]
            [speclj.running :refer [run-directories]]
            [speclj.stub]
            [speclj.tags :refer [describe-filter]]))

(def speclj-invocation
  #?(:clj     (or (System/getProperty "speclj.invocation")
                  "java -cp [...] speclj.main")
     :default ""))

(def arg-spec
  (-> (args/create-args)
      (args/add-multi-parameter "specs" "directory|file|file:line targets to run (default: [spec]). Arguments are unioned: a bare directory runs every spec under it, and a file:line target only narrows when its file is not already covered by a directory arg. Use --focus to bypass other args.")
      (args/add-multi-option "s" "sources" "SOURCES" "directories specifying which sources to refresh (default: [src]).")
      (args/add-switch-option "a" "autotest" "Alias to use the 'vigilant' runner and 'documentation' reporter.")
      (args/add-switch-option "b" "stacktrace" "Output full stacktrace")
      (args/add-switch-option "c" "color" "Show colored (red/green) output.")
      (args/add-switch-option "C" "no-color" "Disable colored output (helpful for writing to file).")
      (args/add-value-option "F" "focus" "FOCUS" "Run only this dir|file|file:line and ignore other spec args. Use when a wrapper (lein spec / bb spec) injects default spec dirs you want to skip.")
      (args/add-switch-option "P" "profile" "Shows execution time for each test (documentation reporter).")
      (args/add-switch-option "p" "omit-pending" "Disable messages about pending specs. The number of pending specs and progress meter will still be shown.")
      (args/add-multi-option "D" "default-spec-dirs" "DEFAULT_SPEC_DIRS" "[INTERNAL USE] Default spec directories (overridden by specs given separately).")
      (args/add-multi-option "f" "reporter" "REPORTER" (str "Specifies how to report spec results. Output will be written to *out*. Multiple reporters are allowed.  Builtin reporters:" endl
                                                            "  [c]lojure-test:   Reporting via clojure.test/report" endl
                                                            "  [d]ocumentation:  Includes description/context and characteristic\n                    names" endl
                                                            "  [p]rogress:       (default) Dots" endl
                                                            "  [s]ilent:         No output" endl))
      (args/add-multi-option "f" "format" "FORMAT" "An alias for reporter.")
      (args/add-value-option "r" "runner" "RUNNER" (str "Specifies the spec runner.  Builtin runners:" endl
                                                        "  [s]tandard:  (default) Runs all the specs once" endl
                                                        "  [v]igilant:  Watches for file changes and re-runs affected specs (used\n               by autotest)" endl))
      (args/add-multi-option "t" "tag" "TAG" "Run only the characteristics with the specified tag(s).\nTo exclude characteristics, prefix the tag with ~ (eg ~slow).  Use this option multiple times to filter multiple tags.")
      (args/add-switch-option "v" "version" "Shows the current speclj version.")
      (args/add-switch-option "h" "help" "You're looking at it.")))

(defn- resolve-reporter-alias [name]
  (case name
    "c" "clojure-test"
    "d" "documentation"
    "p" "progress"
    "s" "silent"
    name))

(defn- resolve-aliases [options]
  (cond
    (:format options) (recur (dissoc (assoc options :reporter (concat (:reporter options) (:format options))) :format))
    (:autotest options) (recur (dissoc (assoc options :runner "vigilant" :reporter (concat (:reporter options) ["documentation"])) :autotest))
    (:reporter options) (recur (dissoc (assoc options :reporters (map resolve-reporter-alias (:reporter options))) :reporter))
    (= "s" (:runner options)) (recur (assoc options :runner "standard"))
    (= "v" (:runner options)) (recur (assoc options :runner "vigilant"))
    (:no-color options) (recur (dissoc options :color :no-color))
    (:tag options) (recur (dissoc (assoc options :tags (:tag options)) :tag))
    :else options))

(defn usage [errors]
  (when (seq errors)
    (println "ERROR!!!")
    (run! println errors))
  (println)
  (println "Speclj - pronounced \"speckle\": a TDD/BDD framework for Clojure.")
  (println "Copyright (c) 2010-2026 Micah Martin under The MIT Licenses.")
  (println)
  (println "Usage: " speclj-invocation (args/arg-string arg-spec))
  (println)
  (println (args/parameters-string arg-spec))
  (println (args/options-string arg-spec))
  (if (seq errors) -1 0))

(defn get-version []
  #?(:clj     (version/get-version "speclj" "speclj")
     :default ""))

(defn print-version []
  (println (str "speclj " (get-version))))

(defn- split-line-targets
  "Partitions a seq of :specs entries into [all-paths bare-paths targets].
   Each `path:N` entry lands in both `all-paths` (as the path portion) and
   `targets` (path → line). Bare entries (no :N suffix) also join `bare-paths`,
   since those represent user directives that run everything in scope and
   therefore cover any file:line target underneath them."
  [specs]
  (reduce
    (fn [[all bare targets] entry]
      (let [[path line] (line-filter/split-line-spec entry)]
        (if line
          [(conj all path) bare (assoc targets path line)]
          [(conj all entry) (conj bare entry) targets])))
    [[] [] {}]
    specs))

(defn- dir-path? [p]
  #?(:cljs false
     :default (try (.isDirectory (jio/as-file p))
                   (catch #?(:cljr Exception :default Exception) _ false))))

(defn- covers-file?
  "True when `bare` (a dir or file) contains or equals `target`. A dir covers
   every file beneath it; a plain file covers only itself. Path comparisons
   are canonical so `./spec` and `spec` match."
  [target bare]
  #?(:cljs false
     :default
     (try
       (let [t (.getCanonicalPath (jio/as-file target))
             b (.getCanonicalPath (jio/as-file bare))]
         (if (dir-path? bare)
           (or (= t b)
               (clojure.string/starts-with? t (str b (java.io.File/separator))))
           (= t b)))
       (catch #?(:cljr Exception :default Exception) _ false))))

(defn- prune-covered-targets
  "A file:line target is ignored when a bare dir/file arg already promises to
   run it. This keeps CLI wrappers (lein spec / bb spec) that inject default
   spec dirs from accidentally narrowing the suite."
  [targets bares]
  (if (and (seq bares) (seq targets))
    (into {} (remove (fn [[t _]] (some (partial covers-file? t) bares)) targets))
    targets))

(defn- apply-focus
  "`--focus <dir|file|file:line>` overrides :specs and :line-targets so only
   the focused target is considered. Bypasses default-spec-dirs and any
   positional specs."
  [options]
  (let [focus-val   (:focus options)
        [path line] (line-filter/split-line-spec focus-val)
        options     (-> options
                        (dissoc :focus :default-spec-dirs)
                        (assoc :specs [path]))]
    (if line
      (assoc options :line-targets {path line})
      (dissoc options :line-targets))))

(defn parse-args [& args]
  (let [options (resolve-aliases (args/parse arg-spec args))
        options (if (:focus options)
                  (apply-focus options)
                  (let [options (if (:specs options)
                                  options
                                  (set/rename-keys options {:default-spec-dirs :specs}))]
                    (if-let [specs (:specs options)]
                      (let [[paths bares targets] (split-line-targets specs)
                            targets               (prune-covered-targets targets bares)]
                        (cond-> (assoc options :specs paths)
                          (seq targets) (assoc :line-targets targets)))
                      options)))]
    (merge config/default-config options)))

(defn- path-exists? [path]
  #?(:cljs true
     :default (try (.exists (jio/as-file path))
                   (catch #?(:cljr Exception :default Exception) _ true))))

(defn- prune-missing-targets
  "Returns [remaining-specs remaining-targets missing-target-strings].
   Any :line-targets entry whose file isn't present on disk is removed from
   the spec list and returned in the missing seq so the runner can report it
   as an unmatched target without triggering a hard load error."
  [specs line-targets]
  (let [missing-paths (filter (fn [p] (not (path-exists? p))) (keys line-targets))
        missing-set   (set missing-paths)
        missing-strs  (map (fn [p] (str p ":" (get line-targets p))) missing-paths)]
    [(remove missing-set specs)
     (apply dissoc line-targets missing-paths)
     missing-strs]))

(defn do-specs [config]
  (config/with-config config
    (fn []
      (let [unmatched (atom [])]
        (binding [line-filter/*run-unmatched* unmatched]
          (let [[specs targets missing] (prune-missing-targets config/*specs* config/*line-targets*)]
            (swap! unmatched into missing)
            (binding [config/*specs*        specs
                      config/*line-targets* targets]
              (try
                (when-let [filter-msg (describe-filter)]
                  (report-message* config/*reporters* filter-msg))
                (let [directories (concat config/*sources* config/*specs*)
                      failures    (run-directories config/*runner* directories config/*reporters*)]
                  (doseq [target @unmatched]
                    (report-message* config/*reporters*
                                     (str "WARNING: file:line target did not match any loaded spec: " target)))
                  (+ (or failures 0) (count @unmatched)))
                (catch #?(:cljs :default :default Exception) e
                  (print-stack-trace e)
                  (println (stack-trace-str e))
                  -1)))))))))

(defn run
  "Runs specs with the given command-line args. Returns the number of test failures"
  [& args]
  (let [config (apply parse-args args)]
    (cond
      (:*errors config) (usage (:*errors config))
      (:version config) (do (print-version) 0)
      (or (:speclj config) (:help config)) (usage nil)
      :else (or (do-specs config) 0))))

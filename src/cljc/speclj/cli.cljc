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
      (args/add-multi-parameter "specs" "directories/files specifying which specs to run (default: [spec]). Append ':N' to a file path (e.g. foo_spec.clj:42) to run only the enclosing it/context/describe at line N.")
      (args/add-multi-option "s" "sources" "SOURCES" "directories specifying which sources to refresh (default: [src]).")
      (args/add-switch-option "a" "autotest" "Alias to use the 'vigilant' runner and 'documentation' reporter.")
      (args/add-switch-option "b" "stacktrace" "Output full stacktrace")
      (args/add-switch-option "c" "color" "Show colored (red/green) output.")
      (args/add-switch-option "C" "no-color" "Disable colored output (helpful for writing to file).")
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
  "Partitions a seq of :specs entries into [paths, line-targets-map].
   Each entry is probed with `split-line-spec`: entries of the form
   path:N become both a path (added to `paths`) and a {path N} entry in
   line-targets; others pass through unchanged as paths."
  [specs]
  (reduce
    (fn [[paths targets] entry]
      (let [[path line] (line-filter/split-line-spec entry)]
        (if line
          [(conj paths path) (assoc targets path line)]
          [(conj paths entry) targets])))
    [[] {}]
    specs))

(defn parse-args [& args]
  (let [options (resolve-aliases (args/parse arg-spec args))
        options (if (:specs options)
                  options
                  (set/rename-keys options {:default-spec-dirs :specs}))
        options (if-let [specs (:specs options)]
                  (let [[paths targets] (split-line-targets specs)]
                    (cond-> (assoc options :specs paths)
                      (seq targets) (assoc :line-targets targets)))
                  options)]
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

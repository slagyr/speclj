(ns speclj.cli
  (:require #?(:clj [trptcolin.versioneer.core :as version])
            [speclj.args :as args]
            [clojure.set :as set]
            [speclj.config :as config]
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
      (args/add-multi-parameter "specs" "directories/files specifying which specs to run (default: [spec]).")
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
  (println "Copyright (c) 2010-2025 Micah Martin under The MIT Licenses.")
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

(defn parse-args [& args]
  (let [options (resolve-aliases (args/parse arg-spec args))
        options (if (:specs options)
                  options
                  (set/rename-keys options {:default-spec-dirs :specs}))]
    (merge config/default-config options)))

(defn do-specs [config]
  (config/with-config config
    (fn []
      (try
        (when-let [filter-msg (describe-filter)]
          (report-message* config/*reporters* filter-msg))
        (let [directories (concat config/*sources* config/*specs*)]
          (run-directories config/*runner* directories config/*reporters*))
        (catch #?(:cljs :default :default Exception) e
          (print-stack-trace e)
          (println (stack-trace-str e))
          -1)))))

(defn run [& args]
  (let [config (apply parse-args args)]
    (cond
      (:*errors config) (usage (:*errors config))
      (:version config) (do (print-version) 0)
      (or (:speclj config) (:help config)) (usage nil)
      :else (or (do-specs config) 0))))

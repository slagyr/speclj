(ns speclj.cli
  (:require [speclj.args :as args]
            [speclj.config :refer :all]
            [speclj.mmargs :as mmargs]
            [speclj.platform :refer [endl print-stack-trace]]
            [speclj.reporting :refer [report-message* stack-trace-str]]
            [speclj.run.standard]
            [speclj.running :refer [run-directories]]
            [speclj.stub]
            [speclj.tags :refer [describe-filter]]
            [trptcolin.versioneer.core :as version]))

(def speclj-invocation (or (System/getProperty "speclj.invocation") "java -cp [...] speclj.main"))

(def arg-spec
  (doto (mmargs/->Arguments)
    (args/add-multi-parameter "specs" "directories/files specifying which specs to run.")
    (args/add-switch-option "a" "autotest" "Alias to use the 'vigilant' runner and 'documentation' reporter.")
    (args/add-switch-option "b" "stacktrace" "Output full stacktrace")
    (args/add-switch-option "c" "color" "Show colored (red/green) output.")
    (args/add-switch-option "C" "no-color" "Disable colored output (helpful for writing to file).")
    (args/add-switch-option "p" "omit-pending" "Disable messages about pending specs. The number of pending specs and progress meter will still be shown.")
    (args/add-multi-option "D" "default-spec-dirs" "DEFAULT_SPEC_DIRS" "[INTERNAL USE] Default spec directories (overridden by specs given separately).")
    (args/add-multi-option "f" "reporter" "REPORTER" (str "Specifies how to report spec results. Ouput will be written to *out*. Multiple reporters are allowed.  Builtin reporters:" endl
                                                          "  [c]lojure-test:   (reporting via clojure.test/report)" endl
                                                          "  [d]ocumentation:  (description/context and characteristic names)" endl
                                                          "  [p]rogress:       (default - dots)" endl
                                                          "  [s]ilent:         (no output)" endl))
    (args/add-multi-option "f" "format" "FORMAT" "An alias for reporter.")
    (args/add-value-option "r" "runner" "RUNNER" (str "Specifies the spec runner.  Builtin runners:" endl
                                                      "  [s]tandard:  (default) Runs all the specs once" endl
                                                      "  [v]igilant:  Watches for file changes and re-runs affected specs (used by autotest)" endl))
    (args/add-multi-option "t" "tag" "TAG" "Run only the characteristics with the specified tag(s).\nTo exclude characteristics, prefix the tag with ~ (eg ~slow).  Use this option multiple times to filter multiple tags.")
    (args/add-switch-option "v" "version" "Shows the current speclj version.")
    (args/add-switch-option "h" "help" "You're looking at it.")
    (args/add-switch-option "S" "speclj" "You're looking at it.")))

(defn- resolve-runner-alias [name]
  (case name
    "s" "standard"
    "v" "vigilant"
    name))

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
  (println "Copyright (c) 2010-2015 Micah Martin under The MIT Licenses.")
  (println)
  (println "Usage: " speclj-invocation (args/arg-string arg-spec))
  (println)
  (println (args/parameters-string arg-spec))
  (println (args/options-string arg-spec))
  (if (seq errors) -1 0))

(defn print-version []
  (println (str "speclj " (version/get-version "speclj" "speclj"))))

(defn parse-args [& args]
  (let [options (resolve-aliases (args/parse arg-spec args))
        options (if (:specs options)
                  options
                  (clojure.set/rename-keys options {:default-spec-dirs :specs}))]
    (merge default-config options)))

(defn do-specs [config]
  (with-bindings (config-mappings config)
    (try
      (when-let [filter-msg (describe-filter)]
        (report-message* *reporters* filter-msg))
      (run-directories *runner* *specs* *reporters*)
      (catch Exception e
        (print-stack-trace e)
        (println (stack-trace-str e))
        -1))))

(defn run [& args]
  (let [config (apply parse-args args)]
    (cond
      (:*errors config) (usage (:*errors config))
      (:version config) (do (print-version) 0)
      (or (:speclj config) (:help config)) (usage nil)
      :else (or (do-specs config) 0))))

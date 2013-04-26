(ns speclj.main
  (:require [speclj.running :refer [run-directories run-and-report]]
            [speclj.util :refer [endl]]
            [speclj.config :refer :all]
            [speclj.reporting :refer [print-stack-trace report-message*]]
            [speclj.tags :refer [describe-filter]]
            [speclj.version])
  (:import [mmargs Arguments]))

(def speclj-invocation (or (System/getProperty "speclj.invocation") "java -cp [...] speclj.main"))

(def arg-spec (Arguments.))
(doto arg-spec
  (.addMultiParameter "specs" "directories/files specifying which specs to run.")
  (.addSwitchOption "a" "autotest" "Alias to use the 'vigilant' runner and 'specdoc' reporter.")
  (.addSwitchOption "b" "stacktrace" "Output full stacktrace")
  (.addSwitchOption "c" "color" "Show colored (red/green) output.")
  (.addMultiOption "f" "reporter" "REPORTER" (str "Specifies how to report spec results. Ouput will be written to *out*. Multiple reporters are allowed.  Builtin reporters:" endl
                                               "  [d]ocumentation:  (description/context and characteristic names)" endl
                                               "  [p]rogress:       (default - dots)" endl
                                               "  [s]ilent:         (no output)" endl))
  (.addMultiOption "f" "format" "FORMAT" "An alias for reporter.")
  (.addValueOption "r" "runner" "RUNNER" (str "Specifies the spec runner.  Builtin runners:" endl
                                           "  [s]tandard:  (default) Runs all the specs once" endl
                                           "  [v]igilant:  Watches for file changes and re-runs affected specs (used by autotest)" endl))
  (.addMultiOption "t" "tag" "TAG" "Run only the characteristics with the specified tag(s).\nTo exclude characteristics, prefix the tag with ~ (eg ~slow).  Use this option multiple times to filter multiple tags.")
  (.addSwitchOption "v" "version" "Shows the current speclj version.")
  (.addSwitchOption "h" "help" "You're looking at it.")
  )

(defn- resolve-runner-alias [name]
  (cond
    (= "s" name) "standard"
    (= "v" name) "vigilant"
    :else name))

(defn- resolve-reporter-alias [name]
  (cond
    (= "d" name) "documentation"
    (= "p" name) "progress"
    (= "s" name) "silent"
    :else name))

(defn- resolve-aliases [options]
  (cond
    (:format options) (recur (dissoc (assoc options :reporter (concat (:reporter options) (:format options))) :format))
    (:autotest options) (recur (dissoc (assoc options :runner "vigilant" :reporter (concat (:reporter options) ["documentation"])) :autotest))
    (:reporter options) (recur (dissoc (assoc options :reporters (map resolve-reporter-alias (:reporter options))) :reporter))
    (= "s" (:runner options)) (recur (assoc options :runner "standard"))
    (= "v" (:runner options)) (recur (assoc options :runner "vigilant"))
    (:tag options) (recur (dissoc (assoc options :tags (:tag options)) :tag))
    :else options))

(defn #^{:dynamic true} exit [code]
  (System/exit code))

(defn usage [errors]
  (when (seq errors)
    (println "ERROR!!!")
    (doseq [error (seq errors)]
      (println error)))
  (println)
  (println "Speclj - pronounced \"speckle\": a TDD/BDD framework for Clojure.")
  (println "Copyright (c) 2010-2013 Micah Martin under The MIT Licenses.")
  (println)
  (println "Usage: " speclj-invocation (.argString arg-spec))
  (println)
  (println (.parametersString arg-spec))
  (println (.optionsString arg-spec))
  (if (seq errors)
    (exit -1)
    (exit 0)))

(defn print-version []
  (println speclj.version/summary)
  (exit 0))

(defn parse-args [& args]
  (let [parse-result (.parse arg-spec (into-array String args))
        options (reduce (fn [result entry] (assoc result (keyword (.getKey entry)) (.getValue entry))) {} parse-result)
        options (resolve-aliases options)]
    (if-let [errors (options :*errors)]
      (usage errors)
      (merge default-config options))))

(defn do-specs [config]
  (with-bindings (config-mappings config)
    (try
      (if-let [filter-msg (describe-filter)]
        (report-message* *reporters* filter-msg))
      (exit (run-directories *runner* *specs* *reporters*))
      (catch Exception e
        (.printStackTrace e)
        (print-stack-trace e *err*)
        (exit -1)))))

(defn run [& args]
  (let [config (apply parse-args args)]
    (cond
      (:version config) (print-version)
      (:help config) (usage nil)
      :else (do-specs config))))

(defn -main [& args]
  (apply run args))


(ns speclj.main
  (:use
    [speclj.running :only (run-directories run-and-report)]
    [speclj.util :only (endl)]
    [speclj.config]
    [speclj.reporting :only (print-stack-trace)])
  (:require
    [speclj.version])
  (:import
    [mmargs Arguments]))

(def speclj-invocation (or (System/getProperty "speclj.invocation") "java -cp [...] speclj.main"))

(def arg-spec (Arguments.))
(doto arg-spec
  (.addMultiParameter "specs" "directories/files specifying which specs to run.")
  (.addValueOption "r" "runner" "RUNNER" (str "Use a custom Runner. Builtin runners:" endl
    "  standard               : (default) Runs all the specs once" endl
    "  vigilant               : Watches for file changes and re-runs affected specs (used by autotest)" endl))
  (.addValueOption "f" "reporter" "REPORTER" (str "Specifies how to report spec results. Ouput will be written to *out*.
    Builtin reporters:" endl
    "  silent                 : No output" endl
    "  progress               : (default) Text-based progress bar" endl
    "  specdoc                : Code example doc strings" endl))
  (.addValueOption "f" "format" "FORMAT" "An alias for reporter.")
  (.addSwitchOption "b" "stacktrace" "Output full stacktrace")
  (.addSwitchOption "c" "color" "Show colored (red/green) output.")
  (.addSwitchOption "a" "autotest" "Alias to use the 'vigilant' runner and 'specdoc' reporter.")
  (.addSwitchOption "v" "version" "Shows the current speclj version.")
  (.addSwitchOption "h" "help" "You're looking at it.")
  )

(defn- resolve-aliases [options]
  (cond
    (:format options) (recur (dissoc (assoc options :reporter (:format options)) :format))
    (:autotest options) (recur (dissoc (assoc options :runner "vigilant" :reporter "specdoc") :autotest))
    :else options))

(defn exit [code]
  (System/exit code))

(defn usage [errors]
  (if (seq errors)
    (do
      (println "ERROR!!!")
      (doseq [error (seq errors)]
        (println error))))
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
      (exit (run-directories *runner* *specs* *reporter*))
      (catch Exception e
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


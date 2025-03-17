(ns speclj.script.core
  (:require [clojure.java.shell :as shell]))

(defn ns-invoked? []
  (= *file* (System/getProperty "babashka.file")))

(defn resolve-main []
  (-> "-main" symbol resolve))

(defn run-main []
  (apply (resolve-main) *command-line-args*))

(defn when-main []
  (when (ns-invoked?) (run-main)))

(defn system-exit [code] (System/exit code))

(defn sh
  "Wrapper for clojure.java.shell/sh"
  [& args]
  (let [{:keys [out err exit] :as result} (apply shell/sh args)]
    (when (seq out) (println out))
    (when (seq err) (println err))
    (when-not (zero? exit) (system-exit exit))
    result))

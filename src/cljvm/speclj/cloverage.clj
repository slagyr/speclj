(ns speclj.cloverage
  (:require [cloverage.args :as args]
            [cloverage.coverage :as coverage]
            [speclj.cli :as cli]))

;; Assumes that cloverage is already in the classpath.

(defmethod coverage/runner-fn :speclj [opts]
  (let [runner-opts (into {} (:runner-opts opts))]
    (fn [_nses]
      (let [result (apply cli/run (:args runner-opts))]
        {:errors result}))))

(defn -main [& args]
  (let [[speclj-args cloverage-args] (split-with (complement #{"--"}) args)
        speclj-opts    {:runner-opts {:args speclj-args}}
        cloverage-args (concat ["-r" ":speclj"] (rest cloverage-args))
        cloverage-opts (args/parse-args cloverage-args speclj-opts)]
    (coverage/run-main cloverage-opts speclj-opts)))

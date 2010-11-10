(ns speclj.exec)

(deftype RunResult [characteristic seconds failure])

(defn pass-result [characteristic seconds]
  (RunResult. characteristic seconds nil))

(defn fail-result [characteristic seconds failure]
  (RunResult. characteristic seconds failure))

(defn pass? [result]
  (nil? (.failure result)))

(defn fail? [result]
  (.failure result))
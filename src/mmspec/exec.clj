(ns mmspec.exec)

(deftype RunResult [characteristic failure])

(defn pass-result [characteristic]
  (RunResult. characteristic nil))

(defn fail-result [characteristic failure]
  (RunResult. characteristic failure))

(defn pass? [result]
  (nil? (.failure result)))
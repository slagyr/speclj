(ns speclj.version
  (:require
    [clojure.string :as str]))

(def major 2)
(def minor 3)
(def tiny 3)
(def snapshot false)
(def string
  (str
    (str/join "." (filter identity [major minor tiny]))
    (if snapshot "-SNAPSHOT" "")))
(def summary (str "speclj " string))

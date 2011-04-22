(ns speclj.version
  (:require
    [clojure.string :as str]))

(def major 1)
(def minor 3)
(def tiny 1)
(def snapshot true)
(def string
  (str
    (str/join "." (filter identity [major minor tiny]))
    (if snapshot "-SNAPSHOT" "")))
(def summary (str "speclj " string))
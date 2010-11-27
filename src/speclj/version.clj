(ns speclj.version
  (:require
    [clojure.string :as str]))

(def major 1)
(def minor 0)
(def tiny  3)
(def pre   nil)
(def string (str/join "." (filter identity [major minor tiny])))
(def summary (str "speclj " string))
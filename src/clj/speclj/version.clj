(ns speclj.version
  (:require [clojure.string :as str]
            [trptcolin.versioneer.core :as version]))

(def string (version/get-version "speclj" "speclj"))
(def summary (str "speclj " string))

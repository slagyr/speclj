#!/usr/bin/env bb

(ns speclj.script.install-clj
  (:require [speclj.script.core :as script]
            [speclj.script.util :as util]))

(def url "https://download.clojure.org/install/linux-install-1.11.1.1119.sh")

(defn -main []
  (util/install! url))

(script/when-main)

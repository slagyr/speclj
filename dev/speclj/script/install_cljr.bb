#!/usr/bin/env bb

(ns speclj.script.install-cljr
  (:require [speclj.script.core :as script]
            [speclj.script.util :as util]))

(defn -main [& args]
  (util/dotnet-tool-install "Clojure.Main" "1.12.0-alpha10")
  (util/dotnet-tool-install "Clojure.Cljr" "0.1.0-alpha5"))

(script/when-main)
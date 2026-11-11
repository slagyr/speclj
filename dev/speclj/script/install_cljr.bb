#!/usr/bin/env bb

(ns speclj.script.install-cljr
  (:require [speclj.script.core :as script]
            [speclj.script.util :as util]))

(defn -main [& _]
  (util/dotnet-tool-install "Clojure.Main" "1.12.6")
  (util/dotnet-tool-install "Clojure.Cljr" "0.1.0-beta1"))

(script/when-main)
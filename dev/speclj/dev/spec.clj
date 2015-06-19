(ns speclj.dev.spec
  "Run Speclj specs.  Use this instead of the plugin to avoind conflicts with other installed versions."
  (:require [speclj.main :as main]))

(defn -main [& args]
  (apply main/-main args))
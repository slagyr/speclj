(ns speclj.dev.spec
  "Run Speclj specs.  Use this instead of the plugin to avoind conflicts with other installed versions."
  (:require [speclj.cli :as cli]))

(defn -main [& args]
  (apply cli/run "-p" "-c" "-f" "documentation" args))
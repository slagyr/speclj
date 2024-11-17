(ns speclj.main
  (:require [speclj.cli :refer [run]]
            [speclj.platform :refer [exit]]))

(defn -main [& args]
  (exit (apply run args)))

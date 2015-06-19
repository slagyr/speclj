(ns speclj.main
  (:require [speclj.cli :refer [run]]))

(defn -main [& args]
  (System/exit (apply run args)))

(ns leiningen.build
  (:require [cljs.closure]
            [leiningen.core.eval :refer [eval-in-project]]))

(def options {:output-to "js/speclj.js"
              :output-dir "js"
              :optimizations :simple})

(defrecord Sources [paths]
  cljs.closure/Compilable
  (-compile [_ options] (mapcat #(cljs.closure/-compile % options) paths)))

(defn build [project & args]
  (let [sources (vec (:source-paths project))]
    (println "Compiling " (pr-str sources))
    (eval-in-project project
      `(cljs.closure/build (Sources. ~sources) ~options))))
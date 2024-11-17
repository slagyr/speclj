(ns speclj.dev.cljs
  (:require [cljs.build.api :as api]
            [clojure.java.io :as io])
  (:import (cljs.closure Compilable Inputs)))

(def build-options
  {:development {:optimizations  :none
                 :output-to      "target/specs.js"
                 :output-dir     "target/cljs"
                 :sources        ["spec/cljs" "spec/cljc"]
                 :cache-analysis true
                 :source-map     true
                 :pretty-print   true
                 :verbose        true
                 :watch-fn       (fn [] (println "Success!"))
                 }
   :ci          {
                 :cache-analysis false
                 :optimizations  :advanced
                 :output-to      "target/specs.js"
                 :output-dir     "target/cljs"
                 :sources        ["spec/cljs" "spec/cljc"]
                 :pretty-print   false
                 :verbose        false
                 :watch-fn       (fn [] (println "Success!"))
                 }})

(defn ->build-key [build-key]
  (case build-key
    "ci" :ci
    :development))

(defn run-specs []
  (let [process (.exec (Runtime/getRuntime) "node bin/speclj.js")
        output  (.getInputStream process)
        error   (.getErrorStream process)]
    (io/copy output (System/out))
    (io/copy error (System/err))
    (System/exit (.waitFor process))))

(deftype Sources [build-options]
  Inputs
  (-paths [_] (map io/file (:sources build-options)))
  Compilable
  (-compile [_ opts] (mapcat #(cljs.closure/compile-dir (io/file %) opts) (:sources build-options)))
  (-find-sources [_ opts] (mapcat #(cljs.closure/-find-sources % opts) (:sources build-options))))

(defn -main [& args]
  (let [build-key (->build-key (first args))
        build     (get build-options build-key)]
    (api/build (Sources. build) build)
    (run-specs)))

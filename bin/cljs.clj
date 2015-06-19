(require 'cljs.build.api)

(def build-options
  {
   :optimizations  :none
   :output-to      "target/tests.js"
   :output-dir     "target/classes"
   :cache-analysis true
   :source-map     true
   :pretty-print   true
   :verbose        true
   :watch-fn       (fn [] (println "Success!"))})

(cljs.build.api/build "spec" build-options)
(load-file "../src/speclj/version.clj")

(defproject specljs speclj.version/string
  :description "specljs: Pronounced 'speckles', is a Behavior Driven Development framework for ClojureScript."
  :url "http://speclj.com"
  :license {:name "The MIT License"
            :url "file://LICENSE"
            :distribution :repo
            :comments "Copyright 2013 Micah Martin All Rights Reserved."}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :profiles {:cljs {:repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                                   :init-ns speclj.repl}
                    :dependencies [[com.cemerick/piggieback "0.0.4"]]}
             :dev {:dependencies [[org.clojure/clojurescript "0.0-1978"]]}}
  :aliases {"cljs-repl" ["with-profile" "cljs" "repl"]}

  :source-paths ["src/clj"
                 "src/cljs"
                 "src/translated"]
  :plugins [[lein-cljsbuild "0.3.4"
             :exclusions [[org.clojure/clojurescript]]]]
  :jvm-opts ["-Xmx1g" "-server"]
  :cljsbuild {:builds {:dev {:source-paths ["spec/cljs"
                                            "spec/translated"]
                             :compiler {:output-to "js/speclj.js"
                                        :optimizations :whitespace
                                        :pretty-print true
                                        ;                                        :source-map "main.js.map" ; causes Java Heap explosion after a long wait
                                        }
                             :notify-command ["bin/specljs" "js/speclj.js"]}}}

  :eval-in :leiningen
  )

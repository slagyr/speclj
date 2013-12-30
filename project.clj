(load-file "src/clj/speclj/version.clj")

(defproject speclj speclj.version/string
  :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
  :url "http://speclj.com"
  :license {:name "The MIT License"
            :url "file://LICENSE"
            :distribution :repo
            :comments "Copyright 2011-2013 Micah Martin All Rights Reserved."}

  :hooks [cljx.hooks]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.5.1"]
                                   [com.keminglabs/cljx "0.3.1"]]
                   :plugins [[codox "0.6.4"]
                             [com.keminglabs/cljx "0.3.1"]]
                   :cljx {:builds [{:source-paths ["src/cljx"]
                                    :output-path "target/generated/src/clj"
                                    :rules :clj}
                                   {:source-paths ["src/cljx"]
                                    :output-path "target/generated/src/cljs"
                                    :rules :cljs}
                                   {:source-paths ["test/cljx"]
                                    :output-path "target/generated/test/clj"
                                    :rules :clj}
                                   {:source-paths ["test/cljx"]
                                    :output-path "target/generated/test/cljs"
                                    :rules :cljs}]}}

             :clj {:dependencies [[org.clojure/clojure "1.5.1"]
                                  [fresh "1.0.2"]
                                  [mmargs "1.2.0"]]
                   :source-paths   ["target/generated/src/clj" "src/clj"]
                   :test-paths     ["target/generated/spec/clj" "spec/clj"]}

             :cljs {:dependencies [[org.clojure/clojure "1.5.1"]
                                   [org.clojure/clojurescript "0.0-2080"]
                                   [lein-cljsbuild "1.0.0"]
                                   [com.cemerick/clojurescript.test "0.2.1"]]
                    :plugins [[lein-cljsbuild "1.0.0"]]

                    :source-paths   ["target/generated/src/cljs" "src/cljs"]
                    :test-paths     ["target/generated/spec/cljs" "spec/cljs"]

                    :cljsbuild ~(let [test-command ["bin/specljs" "target/tests.js"]]
                                  {:builds {:dev {:compiler {:output-to "target/tests.js"
                                                             :pretty-print true}
                                                  :notify-command test-command}}
                                   :test-commands {"unit" test-command}})

                    :aliases {"clean" ["cljsbuild" "clean"]
                              "compile" ["cljsbuild" "once"]
                              "test" ["do" "clean" "," "compile"]}}}

  :aliases {"cljsbuild" ["with-profile" "cljs" "cljsbuild"]
            "cljx"      ["with-profile" "dev" "cljx"]}

  ;:eval-in-leiningen true
  ;:uberjar-exclusions [#"^clojure/.*"]
  ;:test-paths ["spec"]
  :java-source-paths ["src"]
  ;:javac-options     ["-target" "1.5" "-source" "1.5"]

  )

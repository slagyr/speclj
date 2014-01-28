(load-file "src/clj/speclj/version.clj")

(defproject speclj speclj.version/string
  :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
  :url "http://speclj.com"
  :license {:name "The MIT License"
            :url "file://LICENSE"
            :distribution :repo
            :comments "Copyright 2011-2014 Micah Martin All Rights Reserved."}

  :hooks [cljx.hooks]

  :profiles {
             :dev {:dependencies [[org.clojure/clojure "1.5.1"]
                                   [com.keminglabs/cljx "0.3.1"]]
                   :plugins [ [com.keminglabs/cljx "0.3.1"]]
                   :cljx {:builds [{:source-paths ["src/cljx"]
                                    :output-path "target/generated/src/clj"
                                    :rules :clj}
                                   {:source-paths ["src/cljx"]
                                    :output-path "target/generated/src/cljs"
                                    :rules :cljs}
                                   {:source-paths ["spec/cljx"]
                                    :output-path "target/generated/spec/clj"
                                    :rules :clj}
                                   {:source-paths ["spec/cljx"]
                                    :output-path "target/generated/spec/cljs"
                                    :rules :cljs}]}}

             :production {:dependencies [[org.clojure/clojure "1.5.1"]
                                         [com.keminglabs/cljx "0.3.1"]
                                         [fresh "1.0.2"]
                                         [mmargs "1.2.0"]]
                          :plugins [[com.keminglabs/cljx "0.3.1"]
                                    [lein-cljsbuild "1.0.0"]]

                          :cljx {:builds [{:source-paths ["src/cljx"]
                                           :output-path "target/generated/src/clj"
                                           :rules :clj}
                                          {:source-paths ["src/cljx"]
                                           :output-path "target/generated/src/cljs"
                                           :rules :cljs}
                                          {:source-paths ["spec/cljx"]
                                           :output-path "target/generated/spec/clj"
                                           :rules :clj}
                                          {:source-paths ["spec/cljx"]
                                           :output-path "target/generated/spec/cljs"
                                           :rules :cljs}]}

                          :source-paths   ["target/generated/src/clj" "src/clj"]
                          :resource-paths ["target/generated/src/cljs" "src/cljs"]
                          :java-source-paths ["src/clj"]
                          }

             :clj {:dependencies [[org.clojure/clojure "1.5.1"]
                                  [fresh "1.0.2"]
                                  [mmargs "1.2.0"]]

                   :source-paths   ["target/generated/src/clj" "src/clj"]
                   :test-paths     ["target/generated/spec/clj" "spec/clj"]
                   :java-source-paths ["src/clj"]
                   }

             :cljs {:dependencies
                    [[org.clojure/clojure "1.5.1"]
                                   [org.clojure/tools.reader "0.7.10"] ;necessary for current version of speclj
                                   [org.clojure/clojurescript "0.0-2014"]  ;necessary for current version of speclj
                                   [lein-cljsbuild "1.0.0"]]
                    :plugins [[lein-cljsbuild "1.0.0"]]

                    :source-paths   ["src/cljs" "src/clj" "spec/clj"]

                    :cljsbuild ~(let [test-command ["cljs/bin/specljs" "target/tests.js"]]
                                  {:builds
                                   {:dev {:source-paths ["target/generated/src/cljs" "src/cljs" "target/generated/spec/cljs" "spec/cljs"]
                                          :compiler {:output-to "target/tests.js"
                                                     :pretty-print true}
                                          :notify-command test-command
                                          }}
                                   :test-commands {"unit" test-command}})

                    :aliases {"clean" ["cljsbuild" "clean"]
                              "compile" ["cljsbuild" "once"]
                              "test" ["do" "clean" "," "compile"]}}}

  :aliases {"cljsbuild" ["with-profile" "cljs" "cljsbuild"]
            "cljx"      ["with-profile" "dev" "cljx"]}

  ;:eval-in-leiningen true
  ;:uberjar-exclusions [#"^clojure/.*"]
  :javac-options     ["-target" "1.5" "-source" "1.5"]

  )

(defproject speclj "3.3.2-SNAPSHOT"
            :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
            :url "http://speclj.com"
            :license {:name         "The MIT License"
                      :url          "file://LICENSE"
                      :distribution :repo
                      :comments     "Copyright 2011-2015 Micah Martin All Rights Reserved."}

            :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
            :javac-options ["-target" "1.5" "-source" "1.5"]

            :source-paths ["src"]
            :test-paths ["spec" "dev"]
            :java-source-paths ["src"]

            :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                           [fresh "1.1.2"]
                           [mmargs "1.2.0"]
                           [trptcolin/versioneer "0.1.1"]]

            :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-3308"]]
                             :plugins      [[codox "0.8.11" :exclusions [org.clojure/clojure]]]}}

            :prep-tasks ["javac" "compile"]

            :aliases {"cljs" ["do" "clean," "run" "-m" "speclj.dev.cljs"]
                      "spec" ["do" "run" "-m" "speclj.dev.spec"]
                      "ci"   ["do" "spec," "cljs"]}

            :codox {:src-dir-uri               "http://github.com/slagyr/speclj/blob/3.3.0/"
                    :src-linenum-anchor-prefix "L"}
            )

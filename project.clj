(defproject speclj "3.4.2"
  :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
  :url "http://speclj.com"
  :license {:name         "The MIT License"
            :url          "file://LICENSE"
            :distribution :repo
            :comments     "Copyright 2011-2023 Micah Martin All Rights Reserved."}

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :javac-options ["-target" "1.7" "-source" "1.7"]

  :source-paths ["src"]
  :test-paths ["spec" "dev"]
  :java-source-paths ["src"]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [fresh "1.1.2"]
                 [mmargs "1.2.0"]
                 [trptcolin/versioneer "0.1.1"]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.11.4"]]}}
  :plugins [[lein-codox "0.10.8"]]

  :prep-tasks ["javac" "compile"]

  :aliases {"cljs" ["do" "clean," "run" "-m" "speclj.dev.cljs"]
            "spec" ["do" "run" "-m" "speclj.dev.spec"]
            "ci"   ["do" "spec," "cljs"]}

  :codox {;:namespaces  [speclj.core]
          :output-path "doc"
          :source-uri  "https://github.com/slagyr/speclj/blob/{version}/{filepath}#L{line}"}
  )

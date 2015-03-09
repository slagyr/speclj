(defproject speclj "3.2.0"
  :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
  :url "http://speclj.com"
  :license {:name         "The MIT License"
            :url          "file://LICENSE"
            :distribution :repo
            :comments     "Copyright 2011-2014 Micah Martin All Rights Reserved."}

  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :javac-options ["-target" "1.5" "-source" "1.5"]
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["spec/clj" "target/test-classes"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [fresh "1.0.2"]
                 [mmargs "1.2.0"]
                 [trptcolin/versioneer "0.1.1"]]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path  "target/classes"
                   :rules        :clj}
                  {:source-paths ["src/cljx"]
                   :output-path  "target/classes"
                   :rules        :cljs}
                  {:source-paths ["spec/cljx"]
                   :output-path  "target/test-classes"
                   :rules        :clj}
                  {:source-paths ["spec/cljx"]
                   :output-path  "target/test-classes"
                   :rules        :cljs}]}

  :java-source-paths ["src/clj"]

  :profiles {:dev {:dependencies [[com.keminglabs/cljx "0.6.0"]
                                  [org.clojure/clojurescript "0.0-3030"]]
                   :plugins      [[com.keminglabs/cljx "0.6.0"]
                                  [org.clojure/clojurescript "0.0-3030"]
                                  [lein-cljsbuild "1.0.5"]]}}

  :cljsbuild {:builds        {:dev {:source-paths   ["target/classes" "src/cljs" "target/test-classes" "spec/cljs"]
                                    :compiler       {:output-to    "target/tests.js"
                                                     :pretty-print true}
                                    :notify-command ["phantomjs" "bin/specljs" "target/tests.js"]
                                    }}
              :test-commands {"unit" ["phantomjs" "bin/specljs" "target/tests.js"]}}

  :prep-tasks [["cljx" "once"] "javac" "compile"]

  :aliases {"cljs" ["do" "clean," "cljx," "cljsbuild" "once" "dev"]
            "ci"   ["do" "clean," "javac," "spec," "cljsbuild" "once" "dev"]}

  :eval-in :leiningen ; to recognize spec task
  )

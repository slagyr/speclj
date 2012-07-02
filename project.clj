(load-file "src/speclj/version.clj")

(defproject speclj speclj.version/string
  :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
  :license {:name "The MIT License"
            :url "file://LICENSE"
            :distribution :repo
            :comments "Copyright © 2011-2012 Micah Martin All Rights Reserved."}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [fresh "1.0.2"]
                 [mmargs "1.2.0"]]
  :eval-in-leiningen true
  :test-path "spec"
  :test-paths ["spec"]
  :java-source-path "src"
  :java-source-paths ["src"]
  :uberjar-exclusions [#"^clojure/.*"]
  )

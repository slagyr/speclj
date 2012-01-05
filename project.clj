(require 'speclj.version)

(defproject speclj speclj.version/string
  :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
  :license {:name "The MIT License"
            :url "file://LICENSE"
            :distribution :repo
            :comments "Copyright © 2011 Micah Martin All Rights Reserved."}
  :dependencies [[org.clojure/clojure "1.4.0-alpha3"]
                 [fresh "1.0.2"]
                 [mmargs "1.2.0"]]
  :dev-dependencies []
  :test-path "spec/"
  :java-source-path "src/"
  :uberjar-exclusions [#"^clojure/.*"]
  )

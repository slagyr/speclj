(require 'speclj.version)

(defproject speclj speclj.version/string
  :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
  :license {:name "MIT"
            :url "file://LICENSE"
            :distribution :repo
            :comments "Copyright © 2010 Micah Martin All Rights Reserved."}
  :dependencies [[org.clojure/clojure "1.2.0"]]
  :test-path "spec/"
  :main speclj.main
  :aot [speclj.running speclj.reporting]
;  :java-source-path "src/"
  )

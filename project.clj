(require 'speclj.version)

(defproject speclj speclj.version/string
  :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
  :license {:name "The MIT License"
            :url "file://LICENSE"
            :distribution :repo
            :comments "Copyright © 2010 Micah Martin All Rights Reserved."}
  :dependencies [[org.clojure/clojure "1.2.0"]]
  :dev-dependencies [[lein-clojars "0.6.0"]]
  :test-path "spec/"
  :java-source-path "src/"
  )

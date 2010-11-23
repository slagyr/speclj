(defproject speclj "1.0.0"
  :description "speclj: Pronounced 'speckle', is a Behavior Driven Development framework for Clojure."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [mmargs "1.2.0"]]
  :repositories { "localShared" "file://m2"}
  :test-path "spec/"
  :main speclj.main)

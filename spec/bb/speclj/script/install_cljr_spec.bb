(ns speclj.script.install-cljr-spec
  (:require [speclj.core :refer :all]
            [speclj.script.install-cljr :as sut]
            [speclj.script.spec-helper :as bb-helper]))

(describe "Install Clojure CLR"
  (with-stubs)
  (bb-helper/stub-shell)

  (it "installs Clojure.Main"
    (sut/-main)
    (should-have-invoked :shell/sh {:with ["dotnet" "tool" "install" "--global" "Clojure.Main" "--version" "1.12.0-alpha10"]}))

  (it "installs Clojure.Cljr"
    (sut/-main)
    (should-have-invoked :shell/sh {:with ["dotnet" "tool" "install" "--global" "Clojure.Cljr" "--version" "0.1.0-alpha5"]}))
  )

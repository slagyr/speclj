(ns project.core-spec 
  (:require-macros [speclj.core :refer [describe it should=]])
  (:require [speclj.core]
            [project.core]))

(describe "A ClojureScript test"
  (it "fails. Fix it!"
    (should= 0 1)))


(speclj.core/run-specs)
(ns speclj.platform-spec
  (:require-macros
    [speclj.core :refer [describe it should=]])
  (:require
    [speclj.run.standard :refer [run-specs]]))

(describe "cljs platform-specific bits"
  (it "javascript object stays pristine"
    (should= {} (js->clj (js-obj)))))


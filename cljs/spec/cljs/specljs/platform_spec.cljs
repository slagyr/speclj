(ns specljs.platform-spec
  (:require-macros
    [specljs.core :refer [describe it should=]])
  (:require [specljs.core :as specljs]))

(describe "cljs platform-specific bits"
  (it "javascript object stays pristine"
    (should= {} (js->clj (js-obj)))))

(ns speclj.clj-args-spec
  (:require [speclj.args-helper :as args-helper]
            [speclj.clj-args :as sut]
            [speclj.core #?(:cljs :refer-macros :default :refer) [describe]]))

(describe "Clojure Args"

  (args-helper/test-arguments sut/->Arguments)

  )

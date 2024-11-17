(ns speclj.clj-args-spec
  (:require [speclj.args-helper :as args-helper]
            [speclj.clj-args :as sut]
            [speclj.core #?(:clj :refer :cljs :refer-macros) [describe]]))

(describe "Clojure Args"

  (args-helper/test-arguments sut/->Arguments)

  )

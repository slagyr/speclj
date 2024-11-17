(ns speclj.clj-args-spec
  (:require [speclj.args-helper :as args-helper]
            [speclj.clj-args :as sut]
            [speclj.core :refer :all]))

(describe "Clojure Args"

  (args-helper/test-arguments sut/->Arguments)

  )

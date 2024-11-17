(ns speclj.mmargs-spec
  (:require [speclj.args-helper :as args-helper]
            [speclj.core :refer :all]
            [speclj.mmargs :as sut]))

(describe "Mmargs"

  (args-helper/test-arguments sut/->Arguments)

  )

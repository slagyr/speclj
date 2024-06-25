(ns speclj.freshener-spec
  (:require [speclj.core :refer :all]
            [speclj.freshener :refer :all]
            [speclj.spec-helper :as spec-helper]))

(describe "Freshener"

  (it "returns n"
    (should= 1 (return-n 1))
    )
  )
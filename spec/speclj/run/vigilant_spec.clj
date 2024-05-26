(ns speclj.run.vigilant-spec
  (:require [speclj.core :refer :all]
            [speclj.run.vigilant :refer :all]
            [speclj.spec-helper :as spec-helper]))

(describe "Vigilant Runner"
  (with runner (new-vigilant-runner))

  (it "can be created"
    (should= [] @(.results @runner)))

  (spec-helper/test-description-filtering new-vigilant-runner)
  )

(run-specs)

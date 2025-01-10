(ns speclj.run.vigilant-spec
  (:require [speclj.core :refer :all]
            [speclj.run.vigilant :refer :all]
            [speclj.spec-helper :as spec-helper]))

(describe "Vigilant Runner"
  (spec-helper/test-get-descriptions new-vigilant-runner)
  (spec-helper/test-description-filtering new-vigilant-runner)

  (it "can be created"
    (should= [] @(.results (new-vigilant-runner))))
  )

(run-specs)

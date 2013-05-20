(ns speclj.run.vigilant-spec
  (:require [speclj.core :refer :all]
            [speclj.run.vigilant :refer :all]))

(describe "Vigilant Runner"
  (with runner (new-vigilant-runner))

  (it "can be created"
    (should= [] @(.results @runner)))

  )

(run-specs)
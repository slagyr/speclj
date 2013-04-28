(ns speclj.run.vigilant-spec
  (:require [speclj.core :refer :all]
            [speclj.run.vigilant :refer :all]
            [speclj.run.standard :refer [run-specs]]))

(describe "Vigilant Runner"
  (with runner (new-vigilant-runner))

  (it "can be created"
    (should= [] @(.results @runner)))

  )

(run-specs)
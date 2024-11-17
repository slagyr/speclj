(ns speclj.run.standard_spec
  (:require [speclj.core :refer-macros [after context describe it should= should-fail with]]
            [speclj.run.standard :as sut]
            [speclj.spec-helper :as spec-helper]))

(def initial-armed sut/armed)

(describe "Standard Runner"
  (after (set! sut/armed initial-armed))

  (context "exporting"
    (spec-helper/test-exported-meta sut/run-specs)
    (spec-helper/test-exported-meta sut/armed)
    (spec-helper/test-exported-meta sut/arm)
    (spec-helper/test-exported-meta sut/disarm)
    (spec-helper/test-exported-meta sut/new-standard-runner)
    )

  (it "arms and disarms the runner"
    (sut/disarm)
    (should= false sut/armed)
    (sut/arm)
    (should= true sut/armed)
    (sut/disarm)
    (should= false sut/armed))

  (spec-helper/test-description-filtering sut/new-standard-runner)

  )

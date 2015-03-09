(ns project.core-spec
  (:require [speclj.core :refer :all]
            [project.core :refer :all]))

(describe "Core"

  (it "failing spec"
    (should= 0 1))

  (it "throws an ex-info"
    (throw (ex-info "I'm not a failure" {:foo "bar"})))

  )

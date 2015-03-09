(ns project.core-spec 
  (:require-macros [speclj.core :refer [describe it should=]])
  (:require [speclj.core]
            [project.core]))

(describe "Core"

  (it "failing spec"
    (should= 0 1))

  (it "throws an ex-info"
    (throw (ex-info "I'm not a failure!" {:foo "bar"})))

  (it "throws an error"
    (throw (js/Error. "I'm not a failure either!")))

  (it "throws an string"
    (throw "I'm a string!"))

  )


(speclj.core/run-specs)
(ns mmspec.core-test
  (:use [mmspec.core]))


(describe "Fake specs"
  (it "has assertions"
    (should (= 1 1)))

  (it "has a failure"
    (should (= 1 2)))

  (for [i (range 5)]
    (it (str "knows " i " == " i)
      (should (= i (- i 1)))))

  )


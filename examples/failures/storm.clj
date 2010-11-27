(ns storm
  (:use
    [speclj.core]))

(describe "The storm"

  (it "downright fails"
    (should-fail "fails for sure"))

  (it "downright fail again"
    (should-fail))

  (it "fails on a boolean"
    (should false))

  (it "fails on equality"
    (should= 1 2))

  (it "fails on inequality"
    (should-not= 1 1))

  (it "fails to throw"
    (should-throw (+ 1 1)))

  (it "fails to throw the right error"
    (should-throw Exception (throw (Error. "oops"))))

  (it "fails to throw error with the right message"
    (should-throw Exception "Howdy" (throw (Exception. "Hiya")))))


(run-specs)
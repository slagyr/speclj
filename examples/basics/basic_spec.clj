(ns basics-spec
  (:use [speclj.core]))

(describe "Truth"

  (it "is true"
    (should true))

  (it "is not false"
    (should-not false)))

(conclude-single-file-run)
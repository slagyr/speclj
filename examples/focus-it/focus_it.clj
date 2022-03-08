(ns focused
  (:require [speclj.core :refer :all]))

(describe "ONLY THE FOCUSED COMPONENTS WILL BE EXECUTED"

  (it "downright fails"
    (should-fail "fails for sure"))

  (it "downright fail again"
    (should-fail))

  (it "fails on a boolean"
    (should false))

  (it "fails on equality"
    (should= 1 2))

  (focus-it "fails on inequality"                           ; THIS WILL BE EXECUTED
    (should-not= 1 1))

  (context "nested scope"
    (focus-it "fails" (should= 1 2))                        ; THIS WILL BE EXECUTED
    (it "isn't run" (should= 1 2)))

  (it "fails to throw"
    (should-throw (+ 1 1)))

  (it "fails to throw the right error"
    (should-throw Exception (throw (Error. "oops"))))

  (it "fails to throw error with the right message"
    (should-throw Exception "Howdy" (throw (Exception. "Hiya")))))

(describe "Other Spec"
  (focus-it "to run" (should= 1 2)))                        ; THIS WILL BE EXECUTED


; TODO: focus-context
; TODO: focus-describe

(run-specs)
(ns focus
  (:require [speclj.core :refer :all]))

(describe "A"
  (it "no" (should false))
  (focus-context "a"
    (it "yes-1" (should false))
    (focus-it "yes-2" (should false))
    (context "aa"
      (it "yes-3" (should false))))
  (it "no" (should false)))

(describe "B"
  (it "no" (should false))
  (focus-context "b"
    (it "yes-4" (should false))))

(focus-describe "C"
  (it "yes-5" (should false))
  (it "yes-6" (should false)))

(run-specs)
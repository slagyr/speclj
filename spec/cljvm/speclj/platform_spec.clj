(ns speclj.platform-spec
  (:require [speclj.core :refer :all]
            [speclj.platform :as sut]))

(describe "Platform"

  (it "enter-pressed?"
    (with-in-str "\n" (should= true (sut/enter-pressed?)))
    (with-in-str "\r" (should= true (sut/enter-pressed?)))
    (with-in-str "a" (should= false (sut/enter-pressed?)))
    (with-in-str "\t" (should= false (sut/enter-pressed?))))

  )

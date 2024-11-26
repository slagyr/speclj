(ns speclj.platform-spec
  (:require [speclj.core :refer :all]
            [speclj.platform :as sut]))

(defmacro test-enter-pressed [in result]
  `(it (pr-str ~in)
     (with-in-str ~in (should= ~result (sut/enter-pressed?)))))


(describe "Platform"

  (context "enter-pressed?"
    (test-enter-pressed "\n" true)
    (test-enter-pressed "a" false)
    (test-enter-pressed "\t" false))

  )

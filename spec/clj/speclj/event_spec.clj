(ns speclj.event-spec
  (:require [speclj.core :refer :all]
            [speclj.event :as sut]))

(describe "Event"

  (it "new-line"
    (should= 10 sut/new-line))
  )

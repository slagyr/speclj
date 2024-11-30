(ns speclj.interval-spec
  (:require [speclj.core :refer :all]
            [speclj.interval :as sut]
            [speclj.platform :as platform]
            [speclj.stub :as stub]
            [speclj.thread :as thread]))

(describe "Interval"
  (with-stubs)

  (it "set-interval loops over handler"
    (let [key (sut/set-interval 50 (stub :interval))]
      (thread/sleep 100)
      (sut/clear-interval key)
      (let [invocations (count (stub/invocations-of :interval))]
        (should<= 2 invocations)
        (should>= 3 invocations))))

  (it "unassigned"
    (should-not-throw (sut/clear-interval "blah")))

  (it "does not sleep for the interval when cleared"
    (let [key   (sut/set-interval 1000 #(thread/sleep 100))
          start (platform/current-time)]
      (thread/sleep 50)
      (sut/clear-interval key)
      (should= 0.1 (platform/secs-since start) 0.01)))

  )

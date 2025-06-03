(ns speclj.interval-spec
  (:require [speclj.core :refer :all]
            [speclj.interval :as sut]
            [speclj.platform :as platform]
            [speclj.stub :as stub]
            [speclj.thread :as thread]))

(describe "Interval"
  (with-stubs)

  (it "set-interval loops over handler"
    (let [key (sut/set-interval 12 (stub :interval))]
      (thread/sleep 25)
      (sut/clear-interval key)
      (let [invocations (count (stub/invocations-of :interval))]
        (should<= 2 invocations)
        (should>= 3 invocations))))

  (it "unassigned"
    (should-not-throw (sut/clear-interval "blah")))

  (it "does not sleep for the interval when cleared"
    (let [key   (sut/set-interval 100 #(thread/sleep 25))
          start (platform/current-time)]
      (thread/sleep 12)
      (sut/clear-interval key)
      (let [secs-since-start (platform/secs-since start)]
        (should<= secs-since-start 0.04)
        (should>= secs-since-start 0.025))))

  )

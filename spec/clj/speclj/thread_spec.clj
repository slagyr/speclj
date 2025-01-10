(ns speclj.thread-spec
  (:require [speclj.core :refer :all]
            [speclj.thread :as sut]))

(declare thread)
(def counter (atom 0))

(describe "Thread"

  (it "create"
    (let [pool-size (count @sut/pool)
          counter   (atom 0)
          thread    (sut/create (swap! counter inc))]
      (should-not (sut/alive? thread))
      (should= 0 @counter)
      (should= pool-size (count @sut/pool))
      (sut/start thread)
      (sut/join thread)
      (should= 1 @counter)))

  (context "spawn"

    (with thread (sut/spawn
                   (sut/sleep 100)
                   (swap! counter inc)))

    (after (sut/interrupt @thread)
           (swap! sut/pool disj @thread))

    (it "creates and starts a thread"
      @thread
      (should= 0 @counter)
      (should (sut/alive? @thread))
      (should-contain @thread @sut/pool)
      (sut/join @thread)
      (should= 1 @counter))
    )
  )

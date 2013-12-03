(ns speclj.stub-spec
  (:require [speclj.core :refer :all]
            [speclj.spec-helper :refer [should-fail! should-pass! failure-message]]
            [speclj.stub :refer :all]
            [speclj.platform :refer [endl]]))

(describe "Stubs"

  (with-stubs)

  (it "are fns"
    (let [stub-fn (stub "foo")]
      (should= true (fn? stub-fn))))

  (it "invocations are recorded"
    (let [stub-fn (stub "fizz")]
      (stub-fn)
      (should= [["fizz" []]] @*stubbed-invocations*)
      (stub-fn :bang)
      (should= [["fizz" []] ["fizz" [:bang]]] @*stubbed-invocations*)
      (stub-fn :foo :bar)
      (should= [["fizz" []] ["fizz" [:bang]] ["fizz" [:foo :bar]]] @*stubbed-invocations*)))

  (it "return values"
    (should= nil ((stub :foo)))
    (should= :bar ((stub :foo {:return :bar})))
    (should= 42 ((stub :foo {:return 42}))))

  (it "can throw stuff"
    (should-throw NullPointerException ((stub :bar {:throw (NullPointerException. "Yay!")})))
    (should-throw IndexOutOfBoundsException ((stub :bar {:throw (IndexOutOfBoundsException. "Oh no!")}))))

  (it "can invoke"
    (let [trail (atom [])
          foo (stub :foo {:invoke #(swap! trail conj :foo)})
          bar (stub :bar {:invoke #(swap! trail conj %1)})
          fizz (stub :fizz {:invoke #(swap! trail conj [%1 %2])})]
      (foo)
      (bar 1)
      (fizz 2 3)
      (should= [:foo 1 [2 3]] @trail)))

  (it "returns result of invoce or explicit return value"
    (should= 9 ((stub :foo {:invoke *}) 3 3))
    (should= 42 ((stub :foo {:invoke * :return 42}) 3 3)))

  (it "invoke with wrong number of params"
    (should-fail! ((stub :foo {:invoke (fn [a] a)})))
    (should= "Stub :foo was invoked with 0 arguments, but the :invoke fn has a different arity"
      (failure-message ((stub :foo {:invoke (fn [a] a)})))))

  (it "throw error when :invoke argument is not a fn"
    (should-throw IllegalArgumentException "stub's :invoke argument must be an ifn" ((stub :foo {:invoke 42}))))

  (context "invocations"
    (with foo (stub :foo))
    (with bar (stub :bar))

    (context "finding"

      (before
        (@foo 1)
        (@foo 2)
        (@bar 3)
        (@bar 4))

      (it "all"
        (should= [[1] [2]] (invocations-of :foo))
        (should= [[3] [4]] (invocations-of :bar)))

      (it "first"
        (should= [1] (first-invocation-of :foo))
        (should= [3] (first-invocation-of :bar)))

      (it "last"
        (should= [2] (last-invocation-of :foo))
        (should= [4] (last-invocation-of :bar)))
      )

    (context "checking"

      (it "at least one invocation"
        (should-fail! (should-have-invoked :foo))
        (should= (str "Expected: an invocation of :foo" endl "     got: 0")
          (failure-message (should-have-invoked :foo)))
        (@foo)
        (should-pass! (should-have-invoked :foo)))

      (it "for no invocations"
        (should-pass! (should-not-have-invoked :foo))
        (@foo)
        (should-fail! (should-not-have-invoked :foo))
        (should= (str "Expected: 0 invocations of :foo" endl "     got: 1")
          (failure-message (should-not-have-invoked :foo))))

      (it "a specific number of invocations"
        (should-fail! (should-have-invoked :foo {:times 1}))
        (should= (str "Expected: 1 invocation of :foo" endl "     got: 0")
          (failure-message (should-have-invoked :foo {:times 1})))
        (@foo)
        (should-pass! (should-have-invoked :foo {:times 1}))
        (@foo)
        (should-fail! (should-have-invoked :foo {:times 1}))
        (should= (str "Expected: 1 invocation of :foo" endl "     got: 2")
          (failure-message (should-have-invoked :foo {:times 1}))))

      (it "with no parameters - failing"
        (@foo 1)
        (should-fail! (should-have-invoked :foo {:with []}))
        (should= (str "Expected: invocation of :foo with []" endl "     got: [1]")
          (failure-message (should-have-invoked :foo {:with []}))))

      (it "with no parameters - passing"
        (@foo)
        (should-pass! (should-have-invoked :foo {:with nil}))
        (should-pass! (should-have-invoked :foo {:with []})))

      (it "with some parameters - failing"
        (@foo 3 2 1)
        (should-fail! (should-have-invoked :foo {:with [1 2 3]}))
        (should= (str "Expected: invocation of :foo with [1 2 3]" endl "     got: [3 2 1]")
          (failure-message (should-have-invoked :foo {:with [1 2 3]}))))

      (it "with some parameters - passing"
        (@foo 1 2 3)
        (should-pass! (should-have-invoked :foo {:with [1 2 3]})))

      )

    (context "should-invoke"

      (it "stubs and checks simple call - passing"
        (should-pass! (should-invoke println {} (println "Hello!"))))

      (it "stubs and checks simple call - failing"
        (should-fail! (should-invoke println {} "No calls to println :("))
        (should= "Expected: an invocation of println\n     got: 0"
          (failure-message (should-invoke println {} "No calls to println :("))))

      (it "uses options on stubbing and checking - passing"
        (should-pass!
          (should-invoke reverse {:return 42 :times 2}
            (should= 42 (reverse [1 2]))
            (should= 42 (reverse [3 4])))))

      (it "uses options on stubbing and checking - failure"
        (should-fail!
          (should-invoke reverse {:return 42 :times 2}
            (should= 42 (reverse [1 2]))))
        (should= "Expected: 2 invocations of reverse\n     got: 1"
          (failure-message
            (should-invoke reverse {:return 42 :times 2}
              (should= 42 (reverse [1 2]))))))

      (it "stubs and checks it was not called - failing"
        (should-fail! (should-not-invoke println {} (println "Hello!")))
        (should= "Expected: 0 invocations of println\n     got: 1"
          (failure-message (should-not-invoke println {} (println "Hello!")))))

      (it "stubs and checks simple call - passing"
        (should-pass! (should-not-invoke println {} "No calls to println :(")))

      )
    )
  )

(run-specs)
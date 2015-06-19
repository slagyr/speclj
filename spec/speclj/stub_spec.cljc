(ns speclj.stub-spec
  (#?(:clj :require :cljs :require-macros)
    [speclj.core :refer [around before context describe it should= should-throw should-invoke should-have-invoked should-not-invoke should-not-have-invoked with with-stubs stub -new-exception]]
    [speclj.spec-helper :refer [should-fail! should-pass! failure-message]])
  (:require [speclj.stub :refer [*stubbed-invocations* invocations-of first-invocation-of last-invocation-of]]
            [speclj.platform :refer [endl exception]]
            [speclj.run.standard :refer [run-specs]]
            #?(:cljs [clojure.data])))

(defn foo-bar-fn [] nil)

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
    (should-throw exception "Yay!" ((stub :bar {:throw (-new-exception "Yay!")})))
    (should-throw exception "Oh no!" ((stub :bar {:throw (-new-exception "Oh no!")}))))

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

  #?(:clj
     (it "invoke with wrong number of params"
       (should-throw exception "Stub :foo was invoked with 0 arguments, but the :invoke fn has a different arity"
                     ((stub :foo {:invoke (fn [a] a)})))))

  (it "throw error when :invoke argument is not a fn"
    (should-throw exception "stub's :invoke argument must be an ifn" ((stub :foo {:invoke 42}))))

  #?(:clj
     (context "multiple threads"

       (around [it]
         (with-redefs [foo-bar-fn (stub :foo-bar-fn)]
           (it)))

       (it "stubs the functions in the other thread"
         (doto (Thread. (fn [] (foo-bar-fn))) (.start) (.join))
         (should-have-invoked :foo-bar-fn {:times 1}))

       (it "allows should-invoke to work as expected"
         (should-invoke
           foo-bar-fn {:times 1}
           (doto (Thread. (fn [] (foo-bar-fn))) (.start) (.join))))))

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
        (should= (str "Expected: invocation of :foo with []" endl "     got: ([1])")
                 (failure-message (should-have-invoked :foo {:with []}))))

      (it "with no parameters - passing"
        (@foo)
        (should-pass! (should-have-invoked :foo {:with nil}))
        (should-pass! (should-have-invoked :foo {:with []})))

      (it "with some parameters - failing"
        (@foo 3 2 1)
        (should-fail! (should-have-invoked :foo {:with [1 2 3]}))
        (should= (str "Expected: invocation of :foo with [1 2 3]" endl "     got: ([3 2 1])")
                 (failure-message (should-have-invoked :foo {:with [1 2 3]}))))

      (it "with some parameters - passing"
        (@foo 1 2 3)
        (should-pass! (should-have-invoked :foo {:with [1 2 3]})))

      (it "with :* (anything) parameters"
        (@foo 1 2 3)
        (should-pass! (should-have-invoked :foo {:with [:* :* :*]}))
        (should-pass! (should-have-invoked :foo {:with [1 :* :*]}))
        (should-pass! (should-have-invoked :foo {:with [:* 2 :*]}))
        (should-pass! (should-have-invoked :foo {:with [:* :* 3]}))
        (should-fail! (should-have-invoked :foo {:with [0 :* :*]})))

      (it "with fn matchers"
        (@bar nil?)
        (should-pass! (should-have-invoked :bar {:with [nil?]}))
        (should-pass! (should-have-invoked :bar {:with [fn?]}))
        (should-fail! (should-have-invoked :bar {:with [number?]}))
        (@foo 1 2 3)
        (should-pass! (should-have-invoked :foo {:with [#(> % 0) #(> % 0) #(> % 0)]}))
        (should-pass! (should-have-invoked :foo {:with [#(not (nil? %)) #(not (nil? %)) #(not (nil? %))]}))
        (should-fail! (should-have-invoked :foo {:with [#(< 5 %) :* :*]}))
        (should-fail! (should-have-invoked :foo {:with [:* #(nil? %) :*]})))

      (it "calls the same stub two different times with different args"
        (let [f (stub :the-stub)]
          (f :one :two)
          (f :three :four)
          (should-have-invoked :the-stub {:with [:one :two]})
          (should-have-invoked :the-stub {:with [:three :four]})))

      (it "calls the same stub many different times with different args"
        (let [f (stub :the-stub)]
          (f :one :two)
          (f :one :two)
          (f :three :four)
          (f :three :four)
          (f :three :four)
          (should-pass! (should-have-invoked :the-stub {:with [:one :two] :times 2}))
          (should-pass! (should-have-invoked :the-stub {:with [:three :four] :times 3}))))

      (it "fails when the number of times does not match for a given invocation"
        (let [f (stub :the-stub)]
          (f :one :two)
          (f :three :four)
          (f :three :four)
          (should-pass! (should-have-invoked :the-stub {:with [:three :four] :times 2}))
          (should-fail! (should-have-invoked :the-stub {:with [:one :two] :times 2}))
          (should= (str "Expected: 2 invocations of :the-stub with [:one :two]" endl "     got: 1 invocation")
                   (failure-message (should-have-invoked :the-stub {:with [:one :two] :times 2})))
          (should= (str "Expected: 1 invocation of :the-stub with [:three :four]" endl "     got: 2 invocations")
                   (failure-message (should-have-invoked :the-stub {:with [:three :four] :times 1})))))
      )

    (context "should-not-have-invoked"

      (it "fails when the number of times and args matches for a given invocation"
        (let [f (stub :the-stub)]
          (f :one :two)
          (f :three :four)
          (f :three :four)
          (should-fail! (should-not-have-invoked :the-stub {:with [:three :four] :times 2}))
          (should= (str "Expected: :the-stub not to have been invoked 2 times with [:three :four]" endl "     got: 2 invocations")
                   (failure-message (should-not-have-invoked :the-stub {:with [:three :four] :times 2})))
          (should= (str "Expected: :the-stub not to have been invoked 1 time with [:one :two]" endl "     got: 1 invocation")
                   (failure-message (should-not-have-invoked :the-stub {:with [:one :two] :times 1})))))

      (it "passes when the number of times and args does not match for a given invocation"
        (let [f (stub :the-stub)]
          (f :one :two)
          (f :three :four)
          (f :three :four)
          (should-pass! (should-not-have-invoked :the-stub {:with [:one :two] :times 0}))
          (should-pass! (should-not-have-invoked :the-stub {:with [:three :four] :times 1}))))

      (it "passes when the number of times does not match for a given invocation"
        (let [f (stub :the-stub)]
          (f :one :two)
          (f :three :four)
          (f :three :four)
          (should-pass! (should-not-have-invoked :the-stub {:times 0}))
          (should-pass! (should-not-have-invoked :the-stub {:times 1}))
          (should-pass! (should-not-have-invoked :the-stub {:times 2}))
          (should-pass! (should-not-have-invoked :the-stub {:times 4}))))

      (it "fails when the number of times does not match for a given invocation with no args"
        (let [f (stub :the-stub)]
          (f :one :two)
          (f :three :four)
          (f :three :four)
          (should-pass! (should-not-have-invoked :the-stub {:times 4}))
          (should= (str "Expected: :the-stub not to have been invoked 3 times" endl "     got: 3 invocations")
                   (failure-message (should-not-have-invoked :the-stub {:times 3})))))

      (it "passes when the args do not match for a given invocation"
        (let [f (stub :the-stub)]
          (f :one :two)
          (f :three :four)
          (f :three :four)
          (should-pass! (should-not-have-invoked :the-stub {:with [:one :three]}))
          (should-pass! (should-not-have-invoked :the-stub {:with [:one :four]}))
          (should-pass! (should-not-have-invoked :the-stub {:with [:two :four]}))))

      (it "fails when the args match for a given invocation"
        (let [f (stub :the-stub)]
          (f :one :two)
          (f :three :four)
          (f :three :four)
          (should-fail! (should-not-have-invoked :the-stub {:with [:one :two]}))
          (should-fail! (should-not-have-invoked :the-stub {:with [:three :four]}))
          (should= (str "Expected: :the-stub not to have been invoked with [:one :two]" endl "     got: ([:one :two] [:three :four] [:three :four])")
                   (failure-message (should-not-have-invoked :the-stub {:with [:one :two]})))
          (should= (str "Expected: :the-stub not to have been invoked with [:three :four]" endl "     got: ([:one :two] [:three :four] [:three :four])")
                   (failure-message (should-not-have-invoked :the-stub {:with [:three :four]})))))

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

      (it "uses should-not-have-invoked"
        (should-pass! (should-not-invoke println {:times 3}
                                         (println "hello!")
                                         (println "hello again!"))))

      (it "allows for nested should-invoke's"
        (should-pass!
          (should-invoke reverse {:times 1}
            (should-invoke println {:times 1}
              (println "hello!")
              (reverse [1 2])))))

      (it "allows for nested should-not-invoke's"
        (should-pass!
          (should-invoke reverse {:times 2}
            (reverse [1 2])
            (should-not-invoke println {:times 2}
                               (println "hello!")
                               (reverse [1 2])))))

      )
    )
  )

(run-specs)

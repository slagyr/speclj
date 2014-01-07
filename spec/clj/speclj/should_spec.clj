(ns speclj.should-spec
  (:require ;cljs-macros
            [speclj.core :refer [context describe it should should-be-a should-be-nil should-be should-not-be
                                 should-be-same should-contain should-fail should-not should-not-be-a
                                 should-not-be-nil should-not-be-same should-not-contain
                                 should-not-throw should-not= should-not== should-throw
                                 should= should== -to-s]]
            [speclj.spec-helper :refer [should-fail! should-pass! failure-message]]
            [speclj.platform-macros :refer [new-exception]])
  (:require [speclj.platform :refer [endl exception type-name]]
            [speclj.run.standard :refer [run-specs]]))

(describe "Should Assertions: "
  (it "should tests truthy"
    (should-pass! (should true))
    (should-fail! (should false)))

  (it "should-not tests falsy"
    (should-fail! (should-not true))
    (should-pass! (should-not false)))

  (it "should failure message is nice"
    (should= "Expected truthy but was: false" (failure-message (should false)))
    (should= "Expected truthy but was: nil" (failure-message (should nil))))

  (it "should failure message is nice"
    (should= "Expected falsy but was: true" (failure-message (should-not true)))
    (should= "Expected falsy but was: 1" (failure-message (should-not 1))))

  (it "should= tests equality"
    (should-pass! (should= 1 1))
    (should-pass! (should= "hello" "hello"))
    (should-fail! (should= 1 2))
    (should-fail! (should= "hello" "goodbye")))

  (it "should-be tests functionality"
    (should-pass! (should-be empty? []))
    (should-fail! (should-be empty? [1 2 3]))
    (should= "Expected [1 2 3] to satisfy: empty?" (failure-message (should-be empty? [1 2 (+ 1 2)])))
    (should= "Expected [1 2 3] to satisfy: (comp zero? first)" (failure-message (should-be (comp zero? first) [1 2 (+ 1 2)]))))

  (it "should-not-be tests complementary functionality"
    (should-pass! (should-not-be empty? [1 2 3]))
    (should-fail! (should-not-be empty? []))
    (should= "Expected 1 not to satisfy: pos?" (failure-message (should-not-be pos? 1)))
    (should= "Expected 1 not to satisfy: (comp pos? inc)" (failure-message (should-not-be (comp pos? inc) 1))))

  (it "should= checks equality of doubles within a delta"
    (should-pass! (should= 1.0 1.0 0.1))
    (should-pass! (should= 1.0 1.09 0.1))
    (should-pass! (should= 1.0 0.91 0.1))
    (should-fail! (should= 1.0 1.2 0.1))
    (should-pass! (should= 3.141592 3.141592 0.000001))
    (should-pass! (should= 3.141592 3.141593 0.000001))
    (should-fail! (should= 3.141592 3.141594 0.000001)))

  (it "should= failure message is nice"
    (should= (str "Expected: 1" endl "     got: 2 (using =)") (failure-message (should= 1 2))))

  (it "nil is printed as 'nil' instead of blank"
    (should= (str "Expected: 1" endl "     got: nil (using =)") (failure-message (should= 1 nil))))

  (it "should= failure message with delta is nice"
    (should= (str "Expected: 1" endl "     got: 2 (using delta: 0.1)") (failure-message (should= 1 2 0.1))))

  (it "prints lazy seqs nicely"
    (should= (str "Expected: (1 2 3)" endl "     got: (3 2 1) (using =)")
      (failure-message (should= '(1 2 3) (concat '(3) '(2 1))))))

  (it "should_not= tests inequality"
    (should-pass! (should-not= 1 2))
    (should-fail! (should-not= 1 1)))

  (it "should_not= failure message is nice"
    (should=
      (str "Expected: 1" endl "not to =: 1")
      (failure-message (should-not= 1 1))))

  (it "should-be-same tests identity"
    (should-pass! (should-be-same "foo" "foo"))
    (should-pass! (should-be-same 1 1))
    (should-fail! (should-be-same [] ()))
    ;cljs-ignore->
    (should-fail! (should-be-same 1 1.0))
    ;<-cljs-ignore
    )

  (it "should-be-same failure message is nice"
    (should= (str "         Expected: 1" endl "to be the same as: 2 (using identical?)")
      (failure-message (should-be-same 1 2))))

  (it "should-not-be-same tests identity"
    (should-fail! (should-not-be-same "foo" "foo"))
    (should-fail! (should-not-be-same 1 1))
    (should-pass! (should-not-be-same [] ()))
    ;cljs-ignore->
    (should-pass! (should-not-be-same 1 1.0))
    ;<-cljs-ignore
    )

  (it "should-not-be-same failure message is nice"
    (should= (str "             Expected: 1" endl "not to be the same as: 1 (using identical?)")
      (failure-message (should-not-be-same 1 1))))

  (it "should-be-nil checks for equality with nil"
    (should-pass! (should-be-nil nil))
    (should-fail! (should-be-nil true))
    (should-fail! (should-be-nil false)))

  (context "should=="

    (context "numbers"
      (it "tests loose equality"
        (should-pass! (should== 1 1))
        (should-pass! (should== 1 1.0))
        (should-pass! (should== (int 1) (long 1)))
        (should-fail! (should== 1 2))
        (should-fail! (should== 1 2.0)))

      (it "reports the error"
        (let [error (str "Expected: 1" endl "     got: 2 (using ==)")]
          (should= error (failure-message (should== 1 2)))))

      ;cljs-ignore->
      (it "reports the error with floats"
        (let [error (str "Expected: 1.0" endl "     got: 2 (using ==)")]
          (should= error (failure-message (should== 1.0 2)))))
      ;<-cljs-ignore

      )
    

    (context "two collections"
      (it "passes if target contains all items"
        (should-pass! (should== [1 2 3] [1 2 3])))

      (it "passes if target contains all items out of order"
        (should-pass! (should== [1 2 3] [1 3 2])))

      (it "fails if target includes extra items"
        (should-fail! (should== [1 2 3] [1 2 3 4])))

      (it "reports extra items"
        (let [message (str "Expected contents: [1 2 3]" endl "              got: [1 2 3 4]" endl "          missing: []" endl "            extra: [4]")]
          (should= message (failure-message (should== [1 2 3] [1 2 3 4])))))

      (it "fails if target is missing items"
        (should-fail! (should== [1 2 3] [1 2])))

      (it "reports missing items"
        (let [message (str "Expected contents: [1 2 3]" endl "              got: [1 2]" endl "          missing: [3]" endl "            extra: []")]
          (should= message (failure-message (should== [1 2 3] [1 2])))))

      (it "fails if target is missing items and has extra items"
        (should-fail! (should== [1 2 3] [1 2 4])))

      (it "reports missing and extra items"
        (let [message (str "Expected contents: [1 2 3]" endl "              got: [1 2 4]" endl "          missing: [3]" endl "            extra: [4]")]
          (should= message (failure-message (should== [1 2 3] [1 2 4])))))

      (it "fails if there are duplicates in the target"
        (should-fail! (should== [1 5] [1 1 1 5])))

      (it "reports extra duplicates"
        (let [message (str "Expected contents: [1 5]" endl "              got: [1 1 1 5]" endl "          missing: []" endl "            extra: [1 1]")]
          (should= message (failure-message (should== [1 5] [1 1 1 5])))))

      (it "fails if there are duplicates in the expected"
        (should-fail! (should== [1 1 1 5] [1 5])))

      (it "reports missing duplicates"
        (let [message (str "Expected contents: [1 1 1 5]" endl "              got: [1 5]" endl "          missing: [1 1]" endl "            extra: []")]
          (should= message (failure-message (should== [1 1 1 5] [1 5])))))

      (it "prints lazyseqs"
        (let [message (str "Expected contents: (1 1 1 5)" endl "              got: [1 5]" endl "          missing: [1 1]" endl "            extra: []")]
          (should= message (failure-message (should== (lazy-seq [1 1 1 5]) [1 5])))))

      (it "prints lists"
        (let [message (str "Expected contents: (1 1 1 5)" endl "              got: [1 5]" endl "          missing: [1 1]" endl "            extra: []")]
          (should= message (failure-message (should== (list 1 1 1 5) [1 5])))))

      (it "prints sets"
        (let [message (str "Expected contents: [1 1 1 5]" endl "              got: #{1 5}" endl "          missing: [1 1]" endl "            extra: []")]
          (should= message (failure-message (should== [1 1 1 5] #{1 5})))))

      (it "checks equality of maps"
        (should-pass! (should== {:a 1} {:a 1}))
        (should-fail! (should== {:a 1} {:a 1 :b 2}))
        (should= (str "Expected contents: {:a 1}" endl "              got: {:a 1, :b 2}" endl "          missing: nil" endl "            extra: {:b 2}") (failure-message (should== {:a 1} {:a 1 :b 2}))))

      ))

  (context "should-not=="

    (context "numbers"
      (it "tests loose equality"
        (should-fail! (should-not== 1 1))
        (should-fail! (should-not== 1 1.0))
        (should-fail! (should-not== (int 1) (long 1)))
        (should-pass! (should-not== 1 2))
        (should-pass! (should-not== 1 2.0)))

      (it "reports the error"
        (let [error (str " Expected: 1" endl "not to ==: 1 (using ==)")]
          (should= error (failure-message (should-not== 1 1)))))

      ;cljs-ignore->
      (it "reports the error with floats"
        (let [error (str " Expected: 1.0" endl "not to ==: 1 (using ==)")]
          (should= error (failure-message (should-not== 1.0 1)))))
      ;<-cljs-ignore

      )

    (context "two collections"
      (it "fails if target contains all items"
        (should-fail! (should-not== [1 2 3] [1 2 3])))

      (it "fails if target contains all items out of order"
        (should-fail! (should-not== [1 2 3] [1 3 2])))

      (it "passes if target includes extra items"
        (should-pass! (should-not== [1 2 3] [1 2 3 4])))

      (it "passes if target is missing items"
        (should-pass! (should-not== [1 2 3] [1 2])))

      (it "passes if target is missing items and has extra items"
        (should-pass! (should-not== [1 2 3] [1 2 4])))

      (it "passes if there are duplicates in the target"
        (should-pass! (should-not== [1 5] [1 1 1 5])))

      (it "passes if there are duplicates in the expected"
        (should-pass! (should-not== [1 1 1 5] [1 5])))

      (it "prints lazyseqs"
        (let [message (str "Expected contents: (1 5)" endl "   to differ from: [1 5]")]
          (should= message (failure-message (should-not== (lazy-seq [1 5]) [1 5])))))

      (it "prints lists"
        (let [message (str "Expected contents: (1 5)" endl "   to differ from: [1 5]")]
          (should= message (failure-message (should-not== (list 1 5) [1 5])))))

      (it "prints sets"
        (let [message (str "Expected contents: #{1 5}" endl "   to differ from: [1 5]")]
          (should= message (failure-message (should-not== (set [1 5]) [1 5])))))
      )
    )

  (it "should-contain checks for containmentship of precise strings"
    (should-pass! (should-contain "foo" "foobar"))
    (should-fail! (should-contain "foo" "bar"))
    (should-fail! (should-contain "foo" "Foo")))

  (it "should-not-contain checks for non-containmentship of precise strings"
    (should-fail! (should-not-contain "foo" "foobar"))
    (should-pass! (should-not-contain "foo" "bar"))
    (should-pass! (should-not-contain "foo" "Foo")))

  (it "should-contain checks for containmentship of regular expressions"
    (should-pass! (should-contain #"hello.*" "hello, world"))
    (should-fail! (should-contain #"hello.*" "hola!"))
    (should-pass! (should-contain #"tea" "I'm a little teapot"))
    (should-fail! (should-contain #"coffee" "I'm a little teapot")))

  (it "should-not-contain checks for non-containmentship of regular expressions"
    (should-fail! (should-not-contain #"hello.*" "hello, world"))
    (should-pass! (should-not-contain #"hello.*" "hola!"))
    (should-fail! (should-not-contain #"tea" "I'm a little teapot"))
    (should-pass! (should-not-contain #"coffee" "I'm a little teapot")))

  (it "should-contain checks for containmentship of collection items"
    (should-pass! (should-contain "tea" ["i'm" "a" "little" "tea" "pot"]))
    (should-pass! (should-contain "tea" (list "i'm" "a" "little" "tea" "pot")))
    (should-pass! (should-contain "tea" (set ["i'm" "a" "little" "tea" "pot"])))
    (should-pass! (should-contain 1 [1 2 3]))
    (should-fail! (should-contain "coffee" ["i'm" "a" "little" "tea" "pot"]))
    (should-fail! (should-contain "coffee" (list "i'm" "a" "little" "tea" "pot")))
    (should-fail! (should-contain "coffee" (set ["i'm" "a" "little" "tea" "pot"]))))

  (it "should-not-contain checks for non-containmentship of collection items"
    (should-fail! (should-not-contain "tea" ["i'm" "a" "little" "tea" "pot"]))
    (should-fail! (should-not-contain "tea" (list "i'm" "a" "little" "tea" "pot")))
    (should-fail! (should-not-contain "tea" (set ["i'm" "a" "little" "tea" "pot"])))
    (should-fail! (should-not-contain 1 [1 2 3]))
    (should-pass! (should-not-contain "coffee" ["i'm" "a" "little" "tea" "pot"]))
    (should-pass! (should-not-contain "coffee" (list "i'm" "a" "little" "tea" "pot")))
    (should-pass! (should-not-contain "coffee" (set ["i'm" "a" "little" "tea" "pot"]))))

  (it "should-contain checks for containmentship of keys"
    (should-pass! (should-contain "foo" {"foo" :bar}))
    (should-fail! (should-contain :bar {"foo" :bar}))
    (should-pass! (should-contain 1 {"foo" :bar 1 2}))
    (should-fail! (should-contain 2 {"foo" :bar 1 2})))

  (it "should-not-contain checks for non-containmentship of keys"
    (should-fail! (should-not-contain "foo" {"foo" :bar}))
    (should-pass! (should-not-contain :bar {"foo" :bar}))
    (should-fail! (should-not-contain 1 {"foo" :bar 1 2}))
    (should-pass! (should-not-contain 2 {"foo" :bar 1 2})))

  (it "should-contain errors on unhandled types"
    (should-throw exception (str "should-contain doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type 1)) "]")
      (should-contain 1 2)))

  (it "should-not-contain errors on unhandled types"
    (should-throw exception (str "should-not-contain doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type 1)) "]")
      (should-not-contain 1 2)))

  (it "should-not-be-nil checks for inequality with nil"
    (should-fail! (should-not-be-nil nil))
    (should-pass! (should-not-be-nil true))
    (should-pass! (should-not-be-nil false)))

  (it "should-contain handles nil containers gracefully"
    (should-fail! (should-contain "foo" nil))
    (should-fail! (should-contain nil nil)))

  (it "should-not-contain handles nil containers gracefully"
    (should-pass! (should-not-contain "foo" nil))
    (should-pass! (should-not-contain nil nil)))

  (it "should-fail is an automatic failure"
    (should-fail! (should-fail))
    (should= "Forced failure" (failure-message (should-fail))))

  (it "should-fail can take a string as the error message"
    (should-fail! (should-fail "some message"))
    (should= "some message" (failure-message (should-fail "some message"))))

  ;cljs-ignore->
  (it "should-throw tests that any Throwable is thrown"
    (should-pass! (should-throw (throw (java.lang.Throwable. "error"))))
    (should-fail! (should-throw (+ 1 1)))
    (should= (str "Expected " (type-name java.lang.Throwable) " thrown from: (+ 1 1)" endl
               (apply str (take (count (type-name java.lang.Throwable)) (repeat " "))) "              but got: <nothing thrown>")
      (failure-message (should-throw (+ 1 1)))))
  ;<-cljs-ignore

  (it "should-throw can test an expected throwable type"
    (should-pass! (should-throw java.lang.Exception (throw (java.lang.Exception.))))
    ;cljs-ignore->
    (should-pass! (should-throw java.lang.Object (throw (java.lang.Exception.))))
    ;<-cljs-ignore
    (should-fail! (should-throw java.lang.Exception (throw (java.lang.Throwable.))))
    (should-fail! (should-throw java.lang.Exception (+ 1 1)))
    (should= (str "Expected " (type-name java.lang.Exception) " thrown from: (+ 1 1)" endl
               (apply str (take (count (type-name java.lang.Exception)) (repeat " "))) "              but got: <nothing thrown>")
      (failure-message (should-throw java.lang.Exception (+ 1 1))))
    (should= (str "Expected " (type-name java.lang.Exception) " thrown from: (throw (java.lang.Throwable. \"some message\"))" endl
               (apply str (take (count (type-name java.lang.Exception)) (repeat " "))) "              but got: " (pr-str (java.lang.Throwable. "some message")))
      (failure-message (should-throw java.lang.Exception (throw (java.lang.Throwable. "some message"))))))

  (it "should-throw can test the message of the exception"
    (should-pass! (should-throw exception "My message" (throw (new-exception "My message"))))
    (should-fail! (should-throw exception "My message" (throw (new-exception "Not my message"))))
    ;cljs-ignore->
    (should-fail! (should-throw exception "My message" (throw (java.lang.Throwable. "My message"))))
    ;<-cljs-ignore
    (should-fail! (should-throw exception "My message" (+ 1 1)))
    (should= (str "Expected exception message didn't match" endl "Expected: \"My message\"" endl "     got: \"Not my message\" (using =)")
      (failure-message (should-throw java.lang.Exception "My message" (throw (java.lang.Exception. "Not my message"))))))

  (it "should-not-throw tests that nothing was thrown"
    (should-pass! (should-not-throw (+ 1 1)))
    (should-fail! (should-not-throw (throw (java.lang.Throwable. "error"))))
    (should= (str "Expected nothing thrown from: " (pr-str '(throw (java.lang.Throwable. "error"))) endl
               "                     but got: " (pr-str (java.lang.Throwable. "error")))
      (failure-message (should-not-throw (throw (java.lang.Throwable. "error"))))))


  (context "should-be-a"
    (it "passes if the actual form is an instance of the expected type"
      (should-pass! (should-be-a (type 1) 1)))

    ;cljs-ignore->
    (it "passes if the actual derives from the expected type"
      (should-pass! (should-be-a Number (int 1))))
    ;<-cljs-ignore

    (it "fails if the actual form is not an instance of the expected type"
      (should-fail! (should-be-a (type 1) "one")))

    (it "fails with an error message"
      (should=
        (str "Expected 1 to be an instance of: " (-to-s (type :foo)) endl "           but was an instance of: " (-to-s (type 1)) " (using isa?)")
        (failure-message (should-be-a (type :foo) 1))))

    )

  (context "should-not-be-a"

    (it "fails if the actual form is an instance of the expected type"
      (should-fail! (should-not-be-a (type 1) 1)))

    ;cljs-ignore->
    (it "fails if the actual derives from the expected type"
      (should-fail! (should-not-be-a Number (int 1))))
    ;<-cljs-ignore

    (it "passes if the actual form is not an instance of the expected type"
      (should-pass! (should-not-be-a (type 1) "one")))

    (it "fails with an error message"
      (should=
        (str "Expected :bar not to be an instance of " (-to-s (type :bar)) " but was (using isa?)")
        (failure-message (should-not-be-a (type :foo) :bar))))

    )

  )

(run-specs :stacktrace true)

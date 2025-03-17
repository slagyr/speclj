(ns speclj.should-spec
  (:require [speclj.core #?(:cljs :refer-macros :default :refer) [it
                                                                  context
                                                                  describe
                                                                  should should-not
                                                                  should= should-not=
                                                                  should== should-not==
                                                                  should-be should-not-be
                                                                  should-be-a should-not-be-a
                                                                  should-be-nil should-not-be-nil
                                                                  should-be-same should-not-be-same
                                                                  should-contain should-not-contain
                                                                  should-have-count should-not-have-count
                                                                  should-start-with should-not-start-with
                                                                  should-end-with should-not-end-with
                                                                  should-throw should-not-throw
                                                                  should< should<=
                                                                  should> should>=
                                                                  should-fail
                                                                  -to-s -new-throwable -new-exception]]
            [speclj.components :as components]
            [speclj.spec-helper #?(:cljs :refer-macros :default :refer) [should-fail! should-pass! failure-message should-have-assertions]]
            [speclj.platform :refer [endl exception type-name throwable]]
            [speclj.run.standard :as standard]
            [clojure.string :as str]))

(describe "Should Assertions: "

  (context "should"
    (it "tests truthy"
      (should-pass! (should true))
      (should-fail! (should false)))

    (it "failure message is nice"
      (should= "Expected truthy but was: false" (failure-message (should false)))
      (should= "Expected truthy but was: nil" (failure-message (should nil))))

    (it "bumps assertion count"
      (should true)
      (should-have-assertions 1))
    )

  (context "should-not"
    (it "tests falsy"
      (should-fail! (should-not true))
      (should-pass! (should-not false)))

    (it "failure message is nice"
      (should= "Expected falsy but was: true" (failure-message (should-not true)))
      (should= "Expected falsy but was: 1" (failure-message (should-not 1))))

    (it "bumps assertion count"
      (should-not false)
      (should-have-assertions 1))
    )

  (context "should="
    (it "tests equality"
      (should-pass! (should= 1 1))
      (should-pass! (should= "hello" "hello"))
      (should-fail! (should= 1 2))
      (should-fail! (should= "hello" "goodbye")))

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

    (it "bumps assertion count"
      (should= 1 1.01 0.1)
      (should= 1 1)
      (should-have-assertions 2))
    )

  (context "should-be"
    (it "tests functionality"
      (should-pass! (should-be empty? []))
      (should-fail! (should-be empty? [1 2 3]))
      (should= "Expected [1 2 3] to satisfy: empty?" (failure-message (should-be empty? [1 2 (+ 1 2)])))
      (should= "Expected [1 2 3] to satisfy: (comp zero? first)" (failure-message (should-be (comp zero? first) [1 2 (+ 1 2)]))))

    (it "bumps assertion count"
      (should-be empty? [])
      (should-be empty? [])
      (should-have-assertions 2))
    )

  (context "should-not-be"
    (it "tests complementary functionality"
      (should-pass! (should-not-be empty? [1 2 3]))
      (should-fail! (should-not-be empty? []))
      (should= "Expected 1 not to satisfy: pos?" (failure-message (should-not-be pos? 1)))
      (should= "Expected 1 not to satisfy: (comp pos? inc)" (failure-message (should-not-be (comp pos? inc) 1))))

    (it "bumps assertion count"
      (should-not-be empty? [1 2 3])
      (should-have-assertions 1))
    )

  (context "should-not="
    (it "tests inequality"
      (should-pass! (should-not= 1 2))
      (should-fail! (should-not= 1 1)))

    (it "failure message is nice"
      (should=
        (str "Expected: 1" endl "not to =: 1")
        (failure-message (should-not= 1 1))))

    (it "bumps assertion count"
      (should-not= 1 2)
      (should-not= 1 2)
      (should-have-assertions 2))
    )

  (context "should-be-same"
    (it "tests identity"
      (#?(:bb should-fail! :default should-pass!) (should-be-same "foo" "foo"))
      (should-pass! (should-be-same 1 1))
      (should-fail! (should-be-same [] ()))
      #?(:cljs    (do)
         :default (should-fail! (should-be-same 1 1.0)))
      )

    (it "failure message is nice"
      (should= (str "         Expected: 1" endl "to be the same as: 2 (using identical?)")
               (failure-message (should-be-same 1 2))))

    (it "bumps assertion count"
      (should-be-same 1 1)
      (should-be-same 1 1)
      (should-have-assertions 2))
    )

  (context "should-not-be-same"
    (it "tests identity"
      (should-fail! (should-not-be-same 1 1))
      (should-pass! (should-not-be-same [] ()))
      #?(:bb      (should-pass! (should-not-be-same "foo" "foo"))
         :default (should-fail! (should-not-be-same "foo" "foo")))
      #?(:cljs    (do)
         :default (should-pass! (should-not-be-same 1 1.0))))

    (it "failure message is nice"
      (should= (str "             Expected: 1" endl "not to be the same as: 1 (using identical?)")
               (failure-message (should-not-be-same 1 1))))

    (it "bumps assertion count"
      (should-not-be-same [] ())
      (should-not-be-same [] ())
      (should-have-assertions 2))
    )

  (context "should-be-nil"
    (it "checks for equality with nil"
      (should-pass! (should-be-nil nil))
      (should-fail! (should-be-nil true))
      (should-fail! (should-be-nil false)))

    (it "bumps assertion count"
      (should-be-nil nil)
      (should-be-nil nil)
      (should-have-assertions 2))
    )

  (context "should=="

    (it "bumps assertion count"
      (should== 1 1)
      (should== 1 1)
      (should-have-assertions 2))

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

      (it "reports the error with floats"
        (let [error (str "Expected: 1.0" endl "     got: 2 (using ==)")]
          #?(:cljs    (do)
             :default (should= error (failure-message (should== 1.0 2))))))

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
        (let [lines (str/split-lines (failure-message (should== {:a 1} {:a 1 :b 2})))]
          (should= "Expected contents: {:a 1}" (nth lines 0))
          (should-contain ":b 2" (nth lines 1))
          (should-contain ":a 1" (nth lines 1))
          (should= "          missing: nil" (nth lines 2))
          (should= "            extra: {:b 2}" (nth lines 3))
          )
        ;        (should= (str "Expected contents: {:a 1}" endl "              got: {:b 2, :a 1}" endl "          missing: nil" endl "            extra: {:b 2}")
        ;          (failure-message (should== {:a 1} {:a 1 :b 2})))
        )

      )
    )

  (context "should-not=="

    (it "bumps assertion count"
      (should-not== 1 2)
      (should-not== 1 2)
      (should-have-assertions 2))

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

      (it "reports the error with floats"
        (let [error (str " Expected: 1.0" endl "not to ==: 1 (using ==)")]
          #?(:cljs    (do)
             :default (should= error (failure-message (should-not== 1.0 1))))))

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

  (context "should-contain"
    (it "checks for membership of precise strings"
      (should-pass! (should-contain "foo" "foobar"))
      (should-fail! (should-contain "foo" "bar"))
      (should-fail! (should-contain "foo" "Foo")))

    (it "checks for matching of regular expressions"
      (should-pass! (should-contain #"hello.*" "hello, world"))
      (should-fail! (should-contain #"hello.*" "hola!"))
      (should-pass! (should-contain #"tea" "I'm a little teapot"))
      (should-fail! (should-contain #"coffee" "I'm a little teapot")))

    (it "checks for membership of collection items"
      (should-pass! (should-contain "tea" ["i'm" "a" "little" "tea" "pot"]))
      (should-pass! (should-contain "tea" (list "i'm" "a" "little" "tea" "pot")))
      (should-pass! (should-contain "tea" (set ["i'm" "a" "little" "tea" "pot"])))
      (should-pass! (should-contain 1 [1 2 3]))
      (should-fail! (should-contain "coffee" ["i'm" "a" "little" "tea" "pot"]))
      (should-fail! (should-contain "coffee" (list "i'm" "a" "little" "tea" "pot")))
      (should-fail! (should-contain "coffee" (set ["i'm" "a" "little" "tea" "pot"]))))

    (it "checks for membership of keys"
      (should-pass! (should-contain "foo" {"foo" :bar}))
      (should-fail! (should-contain :bar {"foo" :bar}))
      (should-pass! (should-contain 1 {"foo" :bar 1 2}))
      (should-fail! (should-contain 2 {"foo" :bar 1 2})))

    (it "errors on unhandled types"
      (should-throw exception (str "should-contain doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type 1)) "]")
        (should-contain 1 2)))

    (it "handles nil containers gracefully"
      (should-fail! (should-contain "foo" nil))
      (should-fail! (should-contain nil nil)))

    (it "bumps assertion count"
      (should-contain "foo" "foobar")
      (should-contain "foo" "foobar")
      (should-have-assertions 2))
    )

  (context "should-not-contain"
    (it "checks for non-membership of precise strings"
      (should-fail! (should-not-contain "foo" "foobar"))
      (should-pass! (should-not-contain "foo" "bar"))
      (should-pass! (should-not-contain "foo" "Foo")))

    (it "checks for non-matching of regular expressions"
      (should-fail! (should-not-contain #"hello.*" "hello, world"))
      (should-pass! (should-not-contain #"hello.*" "hola!"))
      (should-fail! (should-not-contain #"tea" "I'm a little teapot"))
      (should-pass! (should-not-contain #"coffee" "I'm a little teapot")))

    (it "checks for non-membership of collection items"
      (should-fail! (should-not-contain "tea" ["i'm" "a" "little" "tea" "pot"]))
      (should-fail! (should-not-contain "tea" (list "i'm" "a" "little" "tea" "pot")))
      (should-fail! (should-not-contain "tea" (set ["i'm" "a" "little" "tea" "pot"])))
      (should-fail! (should-not-contain 1 [1 2 3]))
      (should-pass! (should-not-contain "coffee" ["i'm" "a" "little" "tea" "pot"]))
      (should-pass! (should-not-contain "coffee" (list "i'm" "a" "little" "tea" "pot")))
      (should-pass! (should-not-contain "coffee" (set ["i'm" "a" "little" "tea" "pot"]))))

    (it "checks for non-membership of keys"
      (should-fail! (should-not-contain "foo" {"foo" :bar}))
      (should-pass! (should-not-contain :bar {"foo" :bar}))
      (should-fail! (should-not-contain 1 {"foo" :bar 1 2}))
      (should-pass! (should-not-contain 2 {"foo" :bar 1 2})))

    (it "errors on unhandled types"
      (should-throw exception (str "should-not-contain doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type 1)) "]")
        (should-not-contain 1 2)))

    (it "handles nil containers gracefully"
      (should-pass! (should-not-contain "foo" nil))
      (should-pass! (should-not-contain nil nil)))

    (it "bumps assertion count"
      (should-not-contain "foo" "bar")
      (should-not-contain "foo" "bar")
      (should-have-assertions 2))
    )

  (context "should-have-count"
    (it "checks for an exact count"
      (should-pass! (should-have-count 0 nil))
      (should-pass! (should-have-count 0 []))
      (should-pass! (should-have-count 1 [1]))
      (should-pass! (should-have-count 2 {1 :a 2 :b}))
      (should-pass! (should-have-count 3 "123"))
      (should-pass! (should-have-count 100 (range 100)))

      (should-fail! (should-have-count 2 [1]))
      (should-fail! (should-have-count 2 "a"))
      (should-fail! (should-have-count 2 {1 :a}))
      (should-fail! (should-have-count -2 {1 :a})))

    (it "communicates the failure"
      (let [message (str "Expected count: 42" endl
                         "Actual count:   1" endl
                         "Actual coll:    [1]")]
        (should= message (failure-message (should-have-count 42 [1])))))

    (it "errors on unhandled types"
      (should-throw exception (str "should-have-count doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type :not-countable)) "]")
        (should-have-count 1 :not-countable))
      (should-throw exception (str "should-have-count doesn't know how to handle these types: [" (type-name (type :nan)) " " (type-name (type [])) "]")
        (should-have-count :nan [])))

    (it "bumps assertion count"
      (should-have-count 0 nil)
      (should-have-count 0 [])
      (should-have-assertions 2)))

  (context "should-not-have-count"
    (it "checks for anything but an exact count"
      (should-pass! (should-not-have-count 1 nil))
      (should-pass! (should-not-have-count 1 []))
      (should-pass! (should-not-have-count 2 [1]))
      (should-pass! (should-not-have-count 3 {1 :a 2 :b}))
      (should-pass! (should-not-have-count 4 "123"))
      (should-pass! (should-not-have-count 101 (range 100)))

      (should-fail! (should-not-have-count 1 [1]))
      (should-fail! (should-not-have-count 1 "a"))
      (should-fail! (should-not-have-count 1 {1 :a})))

    (it "communicates the failure"
      (let [message (str "Expected count to not equal 9 (but it did!)" endl
                         "Collection: (1 2 3 4 5 6 7 8 9)")]
        (should= message (failure-message (should-not-have-count 9 (range 1 10))))))

    (it "errors on unhandled types"
      (should-throw exception (str "should-not-have-count doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type :not-countable)) "]")
        (should-not-have-count 1 :not-countable))
      (should-throw exception (str "should-not-have-count doesn't know how to handle these types: [" (type-name (type :nan)) " " (type-name (type [])) "]")
        (should-not-have-count :nan [])))

    (it "bumps assertion count"
      (should-not-have-count 1 nil)
      (should-not-have-count 1 [])
      (should-have-assertions 2))
    )

  (context "should-not-be-nil"
    (it "checks for inequality with nil"
      (should-fail! (should-not-be-nil nil))
      (should-pass! (should-not-be-nil true))
      (should-pass! (should-not-be-nil false)))

    (it "bumps assertion count"
      (should-not-be-nil false)
      (should-not-be-nil true)
      (should-have-assertions 2))
    )

  (context "should-fail"
    (it "is an automatic failure"
      (should-fail! (should-fail))
      (should= "Forced failure" (failure-message (should-fail))))

    (it "can take a string as the error message"
      (should-fail! (should-fail "some message"))
      (should= "some message" (failure-message (should-fail "some message"))))

    (it "bumps assertion count"
      (should-fail! (should-fail))
      (should-fail! (should-fail))
      (should-fail! (should-fail))
      (should-have-assertions 3))
    )

  (context "should-start-with"
    (it "checks for prefix in strings"
      (should-pass! (should-start-with "abc" "abcdefg"))
      (should-fail! (should-start-with "abc" "ab"))
      (should-fail! (should-start-with "bcd" "abcdefg"))
      (should= (str "Expected \"abcdefg\" to start\n"
                    "    with \"bcd\"")
               (failure-message (should-start-with "bcd" "abcdefg"))))

    (it "checks for prefix in collections"
      (should-pass! (should-start-with [1 2] [1 2 3 4 5]))
      (should-fail! (should-start-with [2 3] [1 2 3 4 5]))
      (should= (str "Expected [1 2 3 4 5] to start\n"
                    "    with [2 3]")
               (failure-message (should-start-with [2 3] [1 2 3 4 5]))))

    (it "errors on unexpected types"
      (should-throw exception (str "should-start-with doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type 1)) "]")
        (should-start-with 1 2)))

    (it "bumps assertion count"
      (should-start-with "abc" "abcdefg")
      (should-start-with "abc" "abcdefg")
      (should-have-assertions 2))
    )

  (context "should-not-start-with"
    (it "checks for prefix in strings"
      (should-pass! (should-not-start-with "bcd" "abcdefg"))
      (should-fail! (should-not-start-with "abc" "abcdefg"))
      (should= (str "Expected \"abcdefg\" to NOT start\n"
                    "    with \"abc\"")
               (failure-message (should-not-start-with "abc" "abcdefg"))))

    (it "checks for prefix in collections"
      (should-pass! (should-not-start-with [2 3] [1 2 3 4 5]))
      (should-fail! (should-not-start-with [1 2] [1 2 3 4 5]))
      (should= (str "Expected [1 2 3 4 5] to NOT start\n"
                    "    with [1 2]")
               (failure-message (should-not-start-with [1 2] [1 2 3 4 5]))))

    (it "errors on unexpected types"
      (should-throw exception (str "should-not-start-with doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type 1)) "]")
        (should-not-start-with 1 2)))

    (it "bumps assertion count"
      (should-not-start-with "bcd" "abcdefg")
      (should-not-start-with "bcd" "abcdefg")
      (should-have-assertions 2))
    )

  (context "should-end-with"
    (it "checks for prefix in strings"
      (should-pass! (should-end-with "xyz" "tuvwxyz"))
      (should-fail! (should-end-with "xyz" ""))
      (should-fail! (should-end-with "wxy" "tuvwxyz"))
      (should= (str "Expected [tuvwxyz] to end\n"
                    "        with [wxy]")
               (failure-message (should-end-with "wxy" "tuvwxyz"))))

    (it "checks for prefix in collections"
      (should-pass! (should-end-with [4 5] [1 2 3 4 5]))
      (should-fail! (should-end-with [3 4] [1 2 3 4 5]))
      (should= (str "Expected [1 2 3 4 5] to end\n"
                    "          with [3 4]")
               (failure-message (should-end-with [3 4] [1 2 3 4 5]))))

    (it "errors on unexpected types"
      (should-throw exception (str "should-end-with doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type 1)) "]")
        (should-end-with 1 2)))

    (it "bumps assertion count"
      (should-pass! (should-end-with "xyz" "tuvwxyz"))
      (should-pass! (should-end-with "xyz" "tuvwxyz"))
      (should-have-assertions 2))
    )

  (context "should-not-end-with"
    (it "checks for prefix in strings"
      (should-pass! (should-not-end-with "wxy" "tuvwxyz"))
      (should-fail! (should-not-end-with "xyz" "tuvwxyz"))
      (should= (str "Expected [tuvwxyz] to NOT end\n"
                    "        with [xyz]")
               (failure-message (should-not-end-with "xyz" "tuvwxyz"))))

    (it "checks for prefix in collections"
      (should-pass! (should-not-end-with [3 4] [1 2 3 4 5]))
      (should-fail! (should-not-end-with [4 5] [1 2 3 4 5]))
      (should= (str "Expected [1 2 3 4 5] to NOT end\n"
                    "          with [4 5]")
               (failure-message (should-not-end-with [4 5] [1 2 3 4 5]))))

    (it "errors on unexpected types"
      (should-throw exception (str "should-not-end-with doesn't know how to handle these types: [" (type-name (type 1)) " " (type-name (type 1)) "]")
        (should-not-end-with 1 2)))

    (it "bumps assertion count"
      (should-not-end-with "wxy" "tuvwxyz")
      (should-not-end-with "wxy" "tuvwxyz")
      (should-have-assertions 2))
    )

  (context "should-throw"

    #?(:clj
       (it "tests that any Throwable is thrown"
         (should-pass! (should-throw (throw (java.lang.Throwable. "error"))))
         (should-fail! (should-throw (+ 1 1)))
         (should= (str "Expected " (type-name java.lang.Throwable) " thrown from: (+ 1 1)" endl
                       (apply str (take (count (type-name java.lang.Throwable)) (repeat " "))) "              but got: <nothing thrown>")
                  (failure-message (should-throw (+ 1 1)))))
       :cljr
       (it "tests that any Throwable is thrown"
         (should-pass! (should-throw (throw (Exception. "error"))))
         (should-fail! (should-throw (+ 1 1)))
         (should= (str "Expected " (type-name Exception) " thrown from: (+ 1 1)" endl
                       (apply str (take (count (type-name Exception)) (repeat " "))) "              but got: <nothing thrown>")
                  (failure-message (should-throw (+ 1 1))))))

    (it "can test an expected throwable type"
      (should-pass! (should-throw exception (throw (-new-exception))))

      #?(:cljs    (do)
         :default (should-pass! (should-throw Object (throw (Exception.)))))

      #?(:cljr    (should-fail! (should-throw SystemException (throw (-new-throwable))))
         :default (should-fail! (should-throw exception (throw (-new-throwable)))))
      (should-fail! (should-throw exception (+ 1 1)))
      (should= (str "Expected " (type-name exception) " thrown from: (+ 1 1)" endl
                    (apply str (take (count (type-name exception)) (repeat " "))) "              but got: <nothing thrown>")
               (failure-message (should-throw exception (+ 1 1))))
      #?(:cljs
         (should=
           (str "Expected nothing thrown from: " (pr-str '(throw (-new-throwable "some message"))) endl
                "                     but got: #object[String some message]")
           (failure-message (should-not-throw (throw (-new-throwable "some message")))))
         :clj
         (should-contain
           (str "Expected " (type-name exception) " thrown from: (throw (-new-throwable \"some message\"))" endl
                (apply str (repeat (count (type-name exception)) " "))
                "              but got: #error {\n :cause \"some message\"\n :via\n [{:type java.lang.Throwable\n")
           (failure-message (should-throw exception (throw (-new-throwable "some message")))))
         :cljr
         (should-contain
           (str "Expected System.SystemException thrown from: (throw (-new-throwable \"some message\"))" endl
                (apply str (repeat (count "System.SystemException") " "))
                "              but got: #error {\n :cause \"some message\"\n :via\n [{:type System.Exception\n")
           (failure-message (should-throw SystemException (throw (-new-throwable "some message"))))))

      )

    (it "can test the message of the exception with regex"
      (should-pass! (should-throw exception #"[a-zA-Z]" (throw (-new-exception "my message"))))
      (should-fail! (should-throw exception #"[a-zA-Z]" (throw (-new-exception "123456")))))

    (it "can test the message of the exception"
      (should-pass! (should-throw exception "My message" (throw (-new-exception "My message"))))
      (should-fail! (should-throw exception "My message" (throw (-new-exception "Not my message"))))
      (should-fail! (should-throw exception "My message" (throw (throwable "My message"))))
      (should-fail! (should-throw exception "My message" (+ 1 1)))
      (should= (str "Expected exception predicate didn't match" endl "Expected: \"My message\"" endl "     got: \"Not my message\" (using =)")
               (failure-message (should-throw exception "My message" (throw (-new-exception "Not my message"))))))

    (it "can test an exception by calling a passed function"
      (should-pass! (should-throw exception #(empty? (speclj.platform/error-message %)) (throw (-new-exception ""))))
      (should-fail! (should-throw exception #((not (empty? (speclj.platform/error-message %))) (throw (-new-exception "")))))
      (should= (str "Expected exception predicate didn't match" endl "Expected: true" endl "     got: \"Not my message\" (using =)")
               (failure-message (should-throw exception #(speclj.platform/error-message %) (throw (-new-exception "Not my message"))))))

    (it "bumps assertions once for form"
      (should-throw (throw (ex-info "" {})))
      (should-have-assertions 1))

    (it "bumps assertions once for form and type"
      (should-throw #?(:cljs js/Error :default clojure.lang.ExceptionInfo) (throw (ex-info "" {})))
      (should-have-assertions 1))

    (it "bumps assertions twice for form and passing predicate"
      (should-throw #?(:cljs js/Error :default clojure.lang.ExceptionInfo) "" (throw (ex-info "" {})))
      (should-have-assertions 2))

    (it "bumps assertions twice for form and failing predicate"
      (should-fail! (should-throw #?(:cljs js/Error :default clojure.lang.ExceptionInfo) "blah" (throw (ex-info "" {}))))
      (should-have-assertions 2))
    )

  (context "should-not-throw"

    (it "tests that nothing was thrown"
      (should-pass! (should-not-throw (+ 1 1)))
      (should-fail! (should-not-throw (throw (-new-throwable "error"))))
      #?(:cljs
         (should=
           (str "Expected nothing thrown from: " (pr-str '(throw (-new-throwable "error"))) endl
                "                     but got: #object[String error]")
           (failure-message (should-not-throw (throw (-new-throwable "error")))))
         :default
         (should-contain
           (str "Expected nothing thrown from: " (pr-str '(throw (-new-throwable "error"))) endl
                (str "                     but got: #error {\n :cause \"error\"\n :via\n [{:type " (type-name throwable)
                     "\n   :message \"error\"\n"))
           (failure-message (should-not-throw (throw (-new-throwable "error")))))))

    (it "bumps assertion count"
      (should-not-throw (+ 1 1))
      (should-not-throw (+ 2 2))
      (should-have-assertions 2))
    )

  (context "should-be-a"
    (it "passes if the actual form is an instance of the expected type"
      (should-pass! (should-be-a (type 1) 1)))

    (it "passes if the actual derives from the expected type"
      #?(:cljs (do)
         :clj  (should-pass! (should-be-a Number (int 1)))
         :cljr (should-pass! (should-be-a ValueType (int 1)))))

    (it "fails if the actual form is not an instance of the expected type"
      (should-fail! (should-be-a (type 1) "one")))

    (it "fails with an error message"
      (should=
        (str "Expected 1 to be an instance of: " (-to-s (type :foo)) endl "           but was an instance of: " (-to-s (type 1)) " (using isa?)")
        (failure-message (should-be-a (type :foo) 1))))

    (it "bumps assertion count"
      (should-be-a (type 1) 1)
      (should-be-a (type 2) 2)
      (should-have-assertions 2))
    )

  (context "should-not-be-a"

    (it "fails if the actual form is an instance of the expected type"
      (should-fail! (should-not-be-a (type 1) 1)))

    (it "fails if the actual derives from the expected type"
      #?(:cljs (do)
         :clj  (should-fail! (should-not-be-a Number (int 1)))
         :cljr (should-fail! (should-not-be-a ValueType (int 1)))))

    (it "passes if the actual form is not an instance of the expected type"
      (should-pass! (should-not-be-a (type 1) "one")))

    (it "fails with an error message"
      (should=
        (str "Expected :bar not to be an instance of " (-to-s (type :bar)) " but was (using isa?)")
        (failure-message (should-not-be-a (type :foo) :bar))))

    (it "bumps assertion count"
      (should-not-be-a (type "") 1)
      (should-not-be-a (type "") 1)
      (should-not-be-a (type "") 1)
      (should-have-assertions 3))
    )

  (context "should<"
    (it "degenerate cases"
      (should-throw exception (str "should< doesn't know how to handle these types: [nil nil]")
        (should< nil nil))
      (should-throw exception (str "should< doesn't know how to handle these types: [" (type-name (type "a")) " " (type-name (type \a)) "]")
        (should< "a" \a)))

    (it "failure cases"
      (should-fail! (should< 1 0))
      (should-fail! (should< 1 1))
      (should= "expected 2 to be less than 1 but got: (< 2 1)" (failure-message (should< 2 1))))

    (it "passing cases"
      (should-pass! (should< 1 2))
      (should-pass! (should< 1.0 1.000001)))

    (it "bumps assertion count"
      (should< 1 2)
      (should< 1 2)
      (should-have-assertions 2))
    )

  (context "should>"
    (it "degenerate cases"
      (should-throw exception (str "should> doesn't know how to handle these types: [nil nil]")
        (should> nil nil))
      (should-throw exception (str "should> doesn't know how to handle these types: [" (type-name (type "a")) " " (type-name (type \a)) "]")
        (should> "a" \a)))

    (it "failure cases"
      (should-fail! (should> 0 1))
      (should-fail! (should> 1 1))
      (should= "expected 1 to be greater than 2 but got: (> 1 2)" (failure-message (should> 1 2))))

    (it "passing cases"
      (should-pass! (should> 2 1))
      (should-pass! (should> 1.000001 1.0)))

    (it "bumps assertion count"
      (should> 2 1)
      (should> 2 1)
      (should-have-assertions 2))
    )

  (context "should<="
    (it "degenerate cases"
      (should-throw exception (str "should<= doesn't know how to handle these types: [nil nil]")
        (should<= nil nil))
      (should-throw exception (str "should<= doesn't know how to handle these types: [" (type-name (type "a")) " " (type-name (type \a)) "]")
        (should<= "a" \a)))

    (it "failure cases"
      (should-fail! (should<= 1 0))
      (should= "expected 2 to be less than or equal to 1 but got: (<= 2 1)" (failure-message (should<= 2 1))))

    (it "passing cases"
      (should-pass! (should<= 1 1))
      (should-pass! (should<= 1 1.0))
      (should-pass! (should<= 1 2))
      (should-pass! (should<= 1.0 1.000001)))

    (it "bumps assertion count"
      (should<= 1 2)
      (should<= 1 2)
      (should-have-assertions 2))
    )

  (context "should>="
    (it "degenerate cases"
      (should-throw exception (str "should>= doesn't know how to handle these types: [nil nil]")
        (should>= nil nil))
      (should-throw exception (str "should>= doesn't know how to handle these types: [" (type-name (type "a")) " " (type-name (type \a)) "]")
        (should>= "a" \a)))

    (it "failure cases"
      (should-fail! (should>= 0 1))
      (should= "expected 1 to be greater than or equal to 2 but got: (>= 1 2)" (failure-message (should>= 1 2))))

    (it "passing cases"
      (should-pass! (should>= 1 1))
      (should-pass! (should>= 1 1.0))
      (should-pass! (should>= 2 1))
      (should-pass! (should>= 1.000001 1.0)))

    (it "bumps assertion count"
      (should>= 2 1)
      (should>= 2 1)
      (should-have-assertions 2)))

  )

(standard/run-specs :stacktrace true)

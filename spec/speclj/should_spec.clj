(ns speclj.should-spec
  (:use
    [speclj.core]
    [speclj.spec-helper]
    [speclj.util :only (endl)]))

(describe "Should Assertions: "
  (it "should tests truthy"
    (should-pass! (should true))
    (should-fail! (should false)))

  (it "should-not tests falsy"
    (should-fail! (should-not true))
    (should-pass! (should-not false)))

  (it "should failure message is nice"
    (should= "Expected truthy but was: <false>" (failure-message (should false)))
    (should= "Expected truthy but was: nil" (failure-message (should nil))))

  (it "should failure message is nice"
    (should= "Expected falsy but was: <true>" (failure-message (should-not true)))
    (should= "Expected falsy but was: <1>" (failure-message (should-not 1))))

  (it "should= tests equality"
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
    (should= (str "Expected: <1>" endl "     got: <2> (using =)") (failure-message (should= 1 2))))

  (it "nil is printed as 'nil' instead of blank"
    (should= (str "Expected: <1>" endl "     got: nil (using =)") (failure-message (should= 1 nil))))

  (it "should= failure message with delta is nice"
    (should= (str "Expected: <1>" endl "     got: <2> (using delta: 0.1)") (failure-message (should= 1 2 0.1))))

  (it "prints lazy seqs nicely"
    (should= (str "Expected: <(1 2 3)>" endl "     got: <(3 2 1)> (using =)")
      (failure-message (should= '(1 2 3) (concat '(3) '(2 1))))))

  (it "should_not= tests inequality"
    (should-pass! (should-not= 1 2))
    (should-fail! (should-not= 1 1)))

  (it "should_not= failure message is nice"
    (should=
      (str "Expected: <1>" endl "not to =: <1>")
      (failure-message (should-not= 1 1))))

  (it "should-be-same tests identity"
    (should-pass! (should-be-same "foo" "foo"))
    (should-pass! (should-be-same 1 1))
    (should-fail! (should-be-same [] ()))
    (should-fail! (should-be-same 1 1.0)))

  (it "should-be-same failure message is nice"
    (should= (str "         Expected: <1>" endl "to be the same as: <2> (using identical?)")
      (failure-message (should-be-same 1 2))))

  (it "should-not-be-same tests identity"
    (should-fail! (should-not-be-same "foo" "foo"))
    (should-fail! (should-not-be-same 1 1))
    (should-pass! (should-not-be-same [] ()))
    (should-pass! (should-not-be-same 1 1.0)))

  (it "should-not-be-same failure message is nice"
    (should= (str "             Expected: <1>" endl "not to be the same as: <1> (using identical?)")
      (failure-message (should-not-be-same 1 1))))

  (it "should-be-nil checks for equality with nil"
    (should-pass! (should-be-nil nil))
    (should-fail! (should-be-nil true))
    (should-fail! (should-be-nil false)))

  (context "should=="
    (it "passes if target contains all items"
      (should-pass! (should== [1 2 3] [1 2 3])))

    (it "passes if target contains all items out of order"
      (should-pass! (should== [1 2 3] [1 3 2])))

    (it "fails if target includes extra items"
      (should-fail! (should== [1 2 3] [1 2 3 4])))

    (it "reports extra items"
      (let [message (str "Expected collection contained:  <[1 2 3]>" endl "Actual collection contained:    <[1 2 3 4]>" endl "The extra elements were:        <[4]>")]
        (should= message (failure-message (should== [1 2 3] [1 2 3 4])))))

    (it "fails if target is missing items"
      (should-fail! (should== [1 2 3] [1 2])))

    (it "reports missing items"
      (let [message (str "Expected collection contained:  <[1 2 3]>" endl "Actual collection contained:    <[1 2]>" endl "The missing elements were:      <[3]>")]
        (should= message (failure-message (should== [1 2 3] [1 2])))))

    (it "fails if target is missing items and has extra items"
      (should-fail! (should== [1 2 3] [1 2 4])))

    (it "reports missing and extra items"
      (let [message (str "Expected collection contained:  <[1 2 3]>" endl "Actual collection contained:    <[1 2 4]>" endl "The missing elements were:      <[3]>" endl "The extra elements were:        <[4]>")]
        (should= message (failure-message (should== [1 2 3] [1 2 4])))))

    (it "fails if there are duplicates in the target"
      (should-fail! (should== [1 5] [1 1 1 5])))

    (it "reports extra duplicates"
      (let [message (str "Expected collection contained:  <[1 5]>" endl "Actual collection contained:    <[1 1 1 5]>" endl "The extra elements were:        <[1 1]>")]
      (should= message (failure-message (should== [1 5] [1 1 1 5])))))

    (it "fails if there are duplicates in the expected"
      (should-fail! (should== [1 1 1 5] [1 5])))

    (it "reports missing duplicates"
      (let [message (str "Expected collection contained:  <[1 1 1 5]>" endl "Actual collection contained:    <[1 5]>" endl "The missing elements were:      <[1 1]>")]
      (should= message (failure-message (should== [1 1 1 5] [1 5])))))

    (it "prints lazyseqs"
      (let [message (str "Expected collection contained:  <(1 1 1 5)>" endl "Actual collection contained:    <[1 5]>" endl "The missing elements were:      <[1 1]>")]
      (should= message (failure-message (should== (lazy-seq [1 1 1 5]) [1 5])))))

    (it "prints lists"
      (let [message (str "Expected collection contained:  <(1 1 1 5)>" endl "Actual collection contained:    <[1 5]>" endl "The missing elements were:      <[1 1]>")]
      (should= message (failure-message (should== (list 1 1 1 5) [1 5])))))

    (it "prints sets"
      (let [message (str "Expected collection contained:  <[1 1 1 5]>" endl "Actual collection contained:    <#{1 5}>" endl "The missing elements were:      <[1 1]>")]
      (should= message (failure-message (should== [1 1 1 5] #{1 5})))))

    )

  (context "should-not=="
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
      (let [message (str "Expected:       <(1 5)>" endl "not to contain: <[1 5]> (using =)")]
      (should= message (failure-message (should-not== (lazy-seq [1 5]) [1 5])))))

    (it "prints lists"
      (let [message (str "Expected:       <(1 5)>" endl "not to contain: <[1 5]> (using =)")]
      (should= message (failure-message (should-not== (list 1 5) [1 5])))))

    (it "prints sets"
      (let [message (str "Expected:       <#{1 5}>" endl "not to contain: <[1 5]> (using =)")]
      (should= message (failure-message (should-not== (set [1 5]) [1 5])))))

    )

  (it "should-contain checks for containmentship of precise strings"
    (should-pass! (should-contain "foo" "foobar"))
    (should-fail! (should-contain "foo" "bar"))
    (should-fail! (should-contain "foo" "Foo")))

  (it "should-not-be-nil checks for inequality with nil"
    (should-fail! (should-not-be-nil nil))
    (should-pass! (should-not-be-nil true))
    (should-pass! (should-not-be-nil false)))

  (it "should-fail is an automatic failure"
    (should-fail! (should-fail))
    (should= "Forced failure" (failure-message (should-fail))))

  (it "should-fail can take a string as the error message"
    (should-fail! (should-fail "some message"))
    (should= "some message" (failure-message (should-fail "some message"))))

  (it "should-throw tests that any Throwable is thrown"
    (should-pass! (should-throw (throw (Throwable. "error"))))
    (should-fail! (should-throw (+ 1 1)))
    (should= (str "Expected java.lang.Throwable thrown from: (+ 1 1)" endl
      "                                 but got: <nothing thrown>")
      (failure-message (should-throw (+ 1 1)))))

  (it "should-throw can test an expected throwable type"
    (should-pass! (should-throw NullPointerException (throw (NullPointerException.))))
    (should-pass! (should-throw Exception (throw (NullPointerException.))))
    (should-fail! (should-throw NullPointerException (throw (Exception.))))
    (should-fail! (should-throw NullPointerException (+ 1 1)))
    (should= (str "Expected java.lang.NullPointerException thrown from: (+ 1 1)" endl
      "                                            but got: <nothing thrown>")
      (failure-message (should-throw NullPointerException (+ 1 1))))
    (should= (str "Expected java.lang.NullPointerException thrown from: (throw (Exception. \"some message\"))" endl
      "                                            but got: java.lang.Exception: some message")
      (failure-message (should-throw NullPointerException (throw (Exception. "some message"))))))

  (it "should-throw can test the message of the exception"
    (should-pass! (should-throw Exception "My message" (throw (Exception. "My message"))))
    (should-fail! (should-throw Exception "My message" (throw (Exception. "Not my message"))))
    (should-fail! (should-throw Exception "My message" (throw (Error. "My message"))))
    (should-fail! (should-throw Exception "My message" (+ 1 1)))
    (should= (str "Expected exception message didn't match" endl "Expected: <\"My message\">" endl "     got: <\"Not my message\"> (using =)")
      (failure-message (should-throw Exception "My message" (throw (Exception. "Not my message"))))))

  (it "should-not-throw tests that nothing was thrown"
    (should-pass! (should-not-throw (+ 1 1)))
    (should-fail! (should-not-throw (throw (Throwable. "error"))))
    (should= (str "Expected nothing thrown from: (throw (Throwable. \"error\"))" endl
      "                     but got: java.lang.Throwable: error")
      (failure-message (should-not-throw (throw (Throwable. "error"))))))

  )

(run-specs)

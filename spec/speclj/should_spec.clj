(ns speclj.should-spec
  (:use
    [speclj.core]
    [speclj.spec-helper]
    [speclj.util :only (endl)]))

(describe "Should Assertions: "
  (it "should tests truthy"
    (should-pass! (should true))
    (should-fail! (should false)))

  (it "should= tests equality"
    (should-pass! (should= 1 1))
    (should-pass! (should= "hello" "hello"))
    (should-fail! (should= 1 2))
    (should-fail! (should= "hello" "goodbye")))

  (it "should failure message is nice"
    (should= "Expected truthy but was: <false>" (failure-message (should false)))
    (should= "Expected truthy but was: <>" (failure-message (should nil))))

  (it "should= failure message is nice"
    (should=
      (str "Expected: <1>" endl "     got: <2> (using =)")
      (failure-message (should= 1 2))))

  (it "should_not= tests inequality"
    (should-pass! (should-not= 1 2))
    (should-fail! (should-not= 1 1)))

  (it "should_not= failure message is nice"
    (should=
      (str "Expected: <1>" endl "not to =: <1>")
      (failure-message (should-not= 1 1))))

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
    (should= (str "Expected exception message didn't match" endl "Expected: <My message>" endl "     got: <Not my message> (using =)")
             (failure-message (should-throw Exception "My message" (throw (Exception. "Not my message"))))))

  (it "should-not-throw tests that nothing was thrown"
    (should-pass! (should-not-throw (+ 1 1)))
    (should-fail! (should-not-throw (throw (Throwable. "error"))))
    (should= (str "Expected nothing thrown from: (throw (Throwable. \"error\"))" endl
                  "                     but got: java.lang.Throwable: error")
            (failure-message (should-not-throw (throw (Throwable. "error"))))))
  )

(conclude-single-file-run)
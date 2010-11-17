(ns speclj.should-test
  (:use
    [speclj.core]
    [speclj.test-help]
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
  )



(conclude-single-file-run)
(ns speclj.core-spec
  (:use
    [speclj.core]))

(describe "The basic spec structure"
  (it "uses a call to describe to begin a description")
  (it "contains 0 to many 'it' forms to specify characteristics")
  (it "characteristics use 'should' forms to make assertions"
    (should (= 1 1))))

(def *bauble* (atom 2))

(describe "before and after forms"
  (it "allow forms that are evaluated before to each characteristic")
  (before (swap! *bauble* inc))

  (it "allow forms that are evaluated after to each characteristic")
  (after (swap! *bauble* (fn [i] (- i 2))))

  (it "lead the *bauble* to be incremented to 1"
    (should (= 1 @*bauble*)))

  (it "then lead to 0 after decrementing by 2 and incrementing again"
    (should (= 0 @*bauble*)))

  (it ": if I switch the value to 42"
    (swap! *bauble* (fn [_] 42)))

  (it ": ... then the next time it'll (42 - 2 + 1) or 41"
    (should (= 41 @*bauble*))))

(describe "before-all and after-all variants"
  (it "allow begin forms to be evaluated only once before all the characteristics")
  (before-all (swap! *bauble* (fn [_] 42)))

  (it "allow after forms to be evaluated only once after all the characteristics")
  (after-all (swap! *bauble* inc))

  (it ": cause a value of 42 once"
    (should (= 42 @*bauble*)))

  (it ": ... still 42 but we'll dec it"
    (should (= 42 @*bauble*))
    (swap! *bauble* inc))

  (it ": ... now it's 43.  See, the before fn was never called"
    (should (= 43 @*bauble*))))

(describe "the previous after-all form"
  (it "incremented the value to 44"
    (should (= 44 @*bauble*))))

(describe "setting up state for descriptions"
  (it "can be achieved using the 'with' form")
  (with bibelot (String. "shiney"))

  (it ": 'with' forms can be dereferenced in your characteristics"
    (should (= "shiney" @bibelot)))

  (it ": they're evaluated lazily, and only once for each characteristic"
    (should (identical? @bibelot @bibelot)))

  (it ": 'with' forms are reset for each characteristic"
    (swap! *bauble* (fn [_] @bibelot)))

  (it ": ... such that each characteristic gets a fresh evaluation"
    (should (not (identical? @*bauble* @bibelot)))))

(def gewgaw 0)
(describe "around forms"
  (it "allows characteristics to be wrapped by other forms")
  (around [spec]
    (binding [gewgaw 42]
      (spec)))

  (it ": characteristcs will be evaluated within around form"
    (should= 42 gewgaw))
  )

(conclude-single-file-run)

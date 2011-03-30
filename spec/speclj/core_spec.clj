(ns speclj.core-spec
  (:use
    [speclj.core]))

(describe "The basic spec structure"
  (it "uses a call to describe to begin a description")
  (it "contains 0 to many 'it' forms to specify characteristics")
  (it "characteristics use 'should' forms to make assertions"
    (should (= 1 1))))

(describe "some assertions"
  (it "checks identity"
    (should-be-same "foo" "foo")
    (should-not-be-same 1 2)))

(def bauble (atom 2))

(describe "before and after forms"
  (it "allow forms that are evaluated before to each characteristic")
  (before (swap! bauble inc))

  (it "allow forms that are evaluated after to each characteristic")
  (after (swap! bauble (fn [i] (- i 2))))

  (it "lead the bauble to be incremented to 1"
    (should (= 1 @bauble)))

  (it "then lead to 0 after decrementing by 2 and incrementing again"
    (should (= 0 @bauble)))

  (it ": if I switch the value to 42"
    (reset! bauble 42))

  (it ": ... then the next time it'll (42 - 2 + 1) or 41"
    (should (= 41 @bauble))))

(describe "before-all and after-all variants"
  (it "allow begin forms to be evaluated only once before all the characteristics")
  (before-all (reset! bauble 42))

  (it "allow after forms to be evaluated only once after all the characteristics")
  (after-all (swap! bauble inc))

  (it ": cause a value of 42 once"
    (should (= 42 @bauble)))

  (it ": ... still 42 but we'll dec it"
    (should (= 42 @bauble))
    (swap! bauble inc))

  (it ": ... now it's 43.  See, the before fn was never called"
    (should (= 43 @bauble))))

(describe "the previous after-all form"
  (it "incremented the value to 44"
    (should (= 44 @bauble))))

(describe "setting up state for descriptions"
  (it "can be achieved using the 'with' form")
  (with bibelot (String. "shiney"))

  (it ": 'with' forms can be dereferenced in your characteristics"
    (should= "shiney" @bibelot))

  (it ": they're evaluated lazily, and only once for each characteristic"
    (should (identical? @bibelot @bibelot)))

  (it ": 'with' forms are reset for each characteristic"
    (reset! bauble @bibelot))

  (it ": ... such that each characteristic gets a fresh evaluation"
    (should (not (identical? @bauble @bibelot)))))

(def *gewgaw* 0)
(describe "around forms"
  (it "allows characteristics to be wrapped by other forms")
  (around [it]
    (binding [*gewgaw* 42]
      (it)))

  (it ": characteristcs will be evaluated within around form"
    (should= 42 *gewgaw*))

  (context "with before and after"
      (before (should= 42 *gewgaw*))
      (it "executes around all of them")
    )
  )

(def frippery (atom []))
(def gimcrack (atom "gimcrack"))
(context "context"
  (it "is an alias for describe")

  (before-all (swap! frippery conj :before-all-1))
  (after-all (swap! frippery conj :after-all-1))
  (with outside :outside)
  (with bibelot (String. "bibelot"))
  (before (reset! bauble [@outside]))
  (after (swap! bauble rest))
  (around [it] (binding [*gewgaw* (inc *gewgaw*)] (it)))

  (it "works as normal"
    (should= [:outside] @bauble))

  (it "leaves a mark"
    (swap! frippery conj :spec-1))

  (it "uses 1 around"
    (should= 1 *gewgaw*))

  (context "nested context"
    (before-all (swap! frippery conj :before-all-2))
    (after-all (swap! frippery conj :after-all-2))
    (with middle :middle)
    (before (swap! bauble conj @middle))
    (after (swap! bauble rest))
    (around [it] (binding [*gewgaw* (inc *gewgaw*)] (it)))

    (it "includes both befores"
      (should= [:outside :middle] @bauble))

    (it "knows about parent with vars"
      (should= :outside @outside))

    (it "leaves a mark"
      (swap! frippery conj :spec-2))

    (it "uses 2 arounds"
      (should= 2 *gewgaw*))

    (context "more nesting"
      (before-all (swap! frippery conj :before-all-3))
      (after-all (swap! frippery conj :after-all-3))
      (with inside :inside)
      (before (swap! bauble conj @inside))
      (after (swap! bauble rest))
      (around [it] (binding [*gewgaw* (inc *gewgaw*)] (it)))

      (it "includes all befores"
        (should= [:outside :middle :inside] @bauble))

      (it "knows about parent with vars"
        (should= :middle @middle))

      (it "leaves a mark"
        (swap! frippery conj :spec-3))

      (it "uses 3 arounds"
        (should= 3 *gewgaw*))

      (it ": record the the with value"
        (reset! gimcrack @bibelot))

      (it ": the with value has changed"
        (should= @bibelot @gimcrack)
        (should (not (identical? @bibelot @gimcrack))))
    )
  )
)

(describe "Nested contexts"
  (it "execute all the components in the right order"
    (should= :before-all-1 (@frippery 0))
    (should= :spec-1 (@frippery 1))
    (should= :before-all-2 (@frippery 2))
    (should= :spec-2 (@frippery 3))
    (should= :before-all-3 (@frippery 4))
    (should= :spec-3 (@frippery 5))
    (should= :after-all-3 (@frippery 6))
    (should= :after-all-2 (@frippery 7))
    (should= :after-all-1 (@frippery 8)))

  (it "executes all the afters"
    (should= [] @bauble)))


(run-specs)

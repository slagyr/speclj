(ns speclj.core-spec
  (:use
    [speclj.core]))

(describe "The basic spec structure"
  (it "uses a call to describe to begin a description" :filler)
  (it "contains 0 to many 'it' forms to specify characteristics" :filler)
  (it "characteristics use 'should' forms to make assertions"
    (should (= 1 1))))

(describe "some assertions"
  (it "checks identity"
    (should-be-same "foo" "foo")
    (should-not-be-same 1 2)))

(def bauble (atom 2))

(describe "before and after forms"
  (it "allow forms that are evaluated before to each characteristic" :filler)
  (before (swap! bauble inc))

  (it "allow forms that are evaluated after to each characteristic" :filler)
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
  (it "allow begin forms to be evaluated only once before all the characteristics" :filler)
  (before-all (reset! bauble 42))

  (it "allow after forms to be evaluated only once after all the characteristics" :filler)
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
  (it "can be achieved using the 'with' form" :filler)
  (with bibelot (String. "shiney"))

  (it ": 'with' forms can be dereferenced in your characteristics"
    (should= "shiney" @bibelot))

  (it ": they're evaluated lazily, and only once for each characteristic"
    (should (identical? @bibelot @bibelot)))

  (it ": 'with' forms are reset for each characteristic"
    (reset! bauble @bibelot))

  (it ": ... such that each characteristic gets a fresh evaluation"
    (should (not (identical? @bauble @bibelot)))))

(describe "with-all form"
  (with-all widget (reset! bauble 0))
  (with gadget (+ @widget (swap! bauble inc)))

  (it "will execute before the first characteristic"
    (should= 1 @gadget)
    (should= 0 @widget)
    (should= 1 @bauble))

  (it "only executes once"
    (should= 2 @gadget)
    (should= 0 @widget)
    (should= 2 @bauble))
  )

(def #^{:dynamic true} *gewgaw* 0)
(describe "around forms"
  (it "allows characteristics to be wrapped by other forms" :filler)
  (around [it]
    (binding [*gewgaw* 42]
      (it)))

  (it ": characteristcs will be evaluated within around form"
    (should= 42 *gewgaw*))

  (context "with before and after"
      (before (should= 42 *gewgaw*))
      (it "executes around all of them" :filler)
    )
  )


;(def widget (atom 5))
;(describe "around-all form"
;  (around-all [context]
;    (binding [*gewgaw* (swap! widget inc)]
;      (context)))
;
;  (it "executes before all the specs"
;    (should= 6 @widget)
;    (should= 6 *gewgaw*))
;
;  (it "only executes onece"
;    (should= 6 @widget)
;    (should= 6 *gewgaw*))
;
;  (context "nested"
;
;    (around-all [context]
;      (binding [*gewgaw* (swap! widget #(* 3 %))]
;        (context)))
;
;    (around-all [context]
;      (binding [*gewgaw* (swap! widget #(/ % 2))]
;        (context)))
;
;    (it "will all execute before the characteristics"
;      (should= 9 @widget)
;      (should= 9 *gewgaw*))
;
;    (it "and still only execure once"
;      (should= 9 @widget)
;      (should= 9 *gewgaw*))
;
;    )
;  )

(def frippery (atom []))
(def gimcrack (atom "gimcrack"))
(context "context"
  (it "is an alias for describe" :filler)

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

(describe "Tags"
  (tags :one)
  (it "tag :one" :filler)

  (context "child"
    (tags "two")
    (it "tag :one :two" :filler)

    (context "grand-child"
      (tags 'three)
      (it "tag: :one :two :three" :filler)))

  (context "child2"
    (tags :four :five)
    (it "tag :one :three :four" :filler))
  )

(describe "with"
  (def lazy-calls (atom 0))
  (with with-example
         (swap! lazy-calls inc))

  (it "never deref'ed with-example"
    (should= 0 @lazy-calls))

  (it "still hasn't deref'ed with-example during reset"
    (should= 0 @lazy-calls))

  (it "finally deref'ed with-example lazily"
    (should= 1 @with-example)))

(describe "with!"
  (def non-lazy-calls (atom 0))
  (with! with-bang-example
         (swap! non-lazy-calls inc))

  (it "has been deref'ed upon instantiation"
    (should= 1 @non-lazy-calls))

  (it "has been reset and deref'ed, not lazy"
    (should= 2 @non-lazy-calls)))

(describe "with-all"
  (def lazy-with-all-calls (atom 0))
  (with-all with-all-example
            (swap! lazy-with-all-calls inc))

  (it "never deref'ed with-all-example"
    (should= 0 @lazy-with-all-calls))

  (it "still hasn't deref'ed with-example during reset"
    (should= 0 @lazy-with-all-calls))

  (it "finally deref'ed with-all-example lazily"
    (should= 1 @with-all-example)))

(describe "with-all!"
  (def non-lazy-with-all-calls (atom 0))
  (with-all! with-bang-example
         (swap! non-lazy-with-all-calls inc))

  (it "has been deref'ed upon instantiation"
    (should= 1 @non-lazy-with-all-calls))

  (it "has not been reset and deref'ed"
    (should= 1 @non-lazy-with-all-calls)))

;(run-specs :tags ["two"])
(run-specs)

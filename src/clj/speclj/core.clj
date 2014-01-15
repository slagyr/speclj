(ns speclj.core
  "Speclj's API.  It contains nothing but macros, so that it can be used
  in both Clojure and ClojureScript."
  (:require [clojure.data]
            [speclj.util :refer [choose-platform-namespace]]))

;(defmacro setup-platform []
;  `(require
;     ~(if (compiling-cljs?)
;        ''[speclj.platform-cljs-macros :as platform-macros]
;        ''[speclj.platform-clj-macros  :as platform-macros])))

(choose-platform-namespace
  '[speclj.platform-clj-macros  :as platform-macros]
  '[speclj.platform-cljs-macros :as platform-macros])

;(defmacro print-compile-platform []
;  `(prn
;     ~(if (compiling-cljs?)
;        '"cljs"
;        '"clj")))
;
;(print-compile-platform)

;(setup-platform)

(defmacro it
  "body => any forms but aught to contain at least one assertion (should)

  Declares a new characteristic (example in rspec)."
  [name & body]
  (if (seq body)
    `(speclj.components/new-characteristic ~name (fn [] ~@body))
    `(speclj.components/new-characteristic ~name (fn [] (pending)))))

(defmacro xit
  "Syntactic shortcut to make the characteristic pending."
  [name & body]
  `(it ~name (pending) ~@body))

(defmacro describe
  "body => & spec-components

  Declares a new spec.  The body can contain any forms that evaluate to spec compoenents (it, before, after, with ...)."
  [name & components]
  `(let [description# (speclj.components/new-description ~name ~(clojure.core/name (.name *ns*)))]
     (binding [speclj.config/*parent-description* description#]
       (doseq [component# (list ~@components)]
         (speclj.components/install component# description#)))
     (platform-macros/when-not-bound speclj.config/*parent-description*
       (speclj.running/submit-description (speclj.config/active-runner) description#))
     description#))

(defmacro context
  "Same as describe, but should be used to nest testing contexts inside the outer describe.
  Contexts can be nested any number of times."
  [name & components]
  `(describe ~name ~@components))

(defmacro before
  "Declares a function that is invoked before each characteristic in the containing describe scope is evaluated. The body
  may consist of any forms, presumably ones that perform side effects."
  [& body]
  `(speclj.components/new-before (fn [] ~@body)))

(defmacro after
  "Declares a function that is invoked after each characteristic in the containing describe scope is evaluated. The body
  may consist of any forms, presumably ones that perform side effects."
  [& body]
  `(speclj.components/new-after (fn [] ~@body)))

(defmacro around
  "Declares a function that will be invoked around each characteristic of the containing describe scope.
  The characteristic will be passed in and the around function is responsible for invoking it.

  (around [it] (binding [*out* new-out] (it)))
  "
  [binding & body]
  `(speclj.components/new-around (fn ~binding ~@body)))

(defmacro before-all
  "Declares a function that is invoked once before any characteristic in the containing describe scope is evaluated. The
  body may consist of any forms, presumably ones that perform side effects."
  [& body]
  `(speclj.components/new-before-all (fn [] ~@body)))

(defmacro after-all
  "Declares a function that is invoked once after all the characteristics in the containing describe scope have been
  evaluated.  The body may consist of any forms, presumably ones that perform side effects."
  [& body]
  `(speclj.components/new-after-all (fn [] ~@body)))

(defmacro with
  "Declares a reference-able symbol that will be lazily evaluated once per characteristic of the containing
  describe scope.  The body may contain any forms, the last of which will be the value of the dereferenced symbol.

  (with meaning 42)
  (it \"knows the meaining life\" (should= @meaning (the-meaning-of :life)))"
  [name & body]
  (platform-macros/-make-with name body `speclj.components/new-with false))

(defmacro with!
  "Declares a reference-able symbol that will be evaluated immediately and reset once per characteristic of the containing
  describe scope.  The body may contain any forms, the last of which will be the value of the dereferenced symbol.

  (def my-num (atom 0))
  (with! my-with! (swap! my-num inc))
  (it \"increments my-num before being accessed\" (should= 1 @my-num) (should= 2 @my-with!))"
  [name & body]
  (platform-macros/-make-with name body `speclj.components/new-with true))

(defmacro with-all
  "Declares a reference-able symbol that will be lazily evaluated once per context. The body may contain any forms,
   the last of which will be the value of the dereferenced symbol.

  (with-all meaning 42)
  (it \"knows the meaining life\" (should= @meaning (the-meaning-of :life)))"
  [name & body]
  (platform-macros/-make-with name body `speclj.components/new-with-all false))

(defmacro with-all!
  "Declares a reference-able symbol that will be immediately evaluated once per context. The body may contain any forms,
   the last of which will be the value of the dereferenced symbol.

  (def my-num (atom 0))
  (with-all! my-with-all! (swap! my-num inc))
  (it \"increments my-num before being accessed\"
    (should= 1 @my-num)
    (should= 2 @my-with!))
  (it \"only increments my-num once per context\"
    (should= 1 @my-num)
    (should= 2 @my-with!))"
  [name & body]
  (platform-macros/-make-with name body `speclj.components/new-with-all true))

(defmacro -to-s [thing]
  `(if (nil? ~thing) "nil" (pr-str ~thing)))

(defmacro -fail [message]
  `(throw (speclj.platform/new-failure ~message)))

(defmacro should
  "Asserts the truthy-ness of a form"
  [form]
  `(let [value# ~form]
     (if-not value#
       (-fail (str "Expected truthy but was: " (-to-s value#) "")))))

(defmacro should-not
  "Asserts the falsy-ness of a form"
  [form]
  `(let [value# ~form]
     (when value#
       (-fail (str "Expected falsy but was: " (-to-s value#))))))

(defmacro should=
  "Asserts that two forms evaluate to equal values, with the expected value as the first parameter."
  ([expected-form actual-form]
    `(let [expected# ~expected-form actual# ~actual-form]
       (when-not (= expected# actual#)
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "     got: " (-to-s actual#) " (using =)")))))
  ([expected-form actual-form delta-form]
    `(let [expected# ~expected-form actual# ~actual-form delta# ~delta-form]
       (when (platform-macros/expected-larger-than-delta expected# actual# delta#)
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "     got: " (-to-s actual#) " (using delta: " delta# ")"))))))

(defmacro should-be
  "Asserts that a form satisfies a function."
  [f-form actual-form]
  `(let [f# ~f-form actual# ~actual-form]
     (when-not (f# actual#)
       (-fail (str "Expected " (-to-s actual#) " to satisfy: " ~(str f-form))))))

(defmacro should-not-be
  "Asserts that a form does not satisfy a function."
  [f-form actual-form]
  `(let [f# ~f-form actual# ~actual-form]
     (when (f# actual#)
       (-fail (str "Expected " (-to-s actual#) " not to satisfy: " ~(str f-form))))))

(defmacro should-not=
  "Asserts that two forms evaluate to inequal values, with the unexpected value as the first parameter."
  [expected-form actual-form]
  `(let [expected# ~expected-form actual# ~actual-form]
     (when (= expected# actual#)
       (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "not to =: " (-to-s actual#))))))

(defmacro should-be-same
  "Asserts that two forms evaluate to the same object, with the expected value as the first parameter."
  [expected-form actual-form]
  `(let [expected# ~expected-form actual# ~actual-form]
     (if (not (identical? expected# actual#))
       (-fail (str "         Expected: " (-to-s expected#) speclj.platform/endl "to be the same as: " (-to-s actual#) " (using identical?)")))))

(defmacro should-not-be-same
  "Asserts that two forms evaluate to different objects, with the unexpected value as the first parameter."
  [expected-form actual-form]
  `(let [expected# ~expected-form actual# ~actual-form]
     (when (identical? expected# actual#)
       (-fail (str "             Expected: " (-to-s expected#) speclj.platform/endl "not to be the same as: " (-to-s actual#) " (using identical?)")))))

(defmacro should-be-nil
  "Asserts that the form evaluates to nil"
  [form]
  `(should= nil ~form))

(defmacro should-contain
  "Multi-purpose assertion of containment.  Works strings, regular expressions, sequences, and maps.

  (should-contain \"foo\" \"foobar\")            ; looks for sub-string
  (should-contain #\"hello.*\" \"hello, world\") ; looks for regular expression
  (should-contain :foo {:foo :bar})          ; looks for a key in a map
  (should-contain 3 [1 2 3 4])               ; looks for an object in a collection"
  [expected actual]
  `(let [expected# ~expected
         actual# ~actual]
     (cond
       (nil? actual#) (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "to be in: nil"))
       (and (string? expected#) (string? actual#))
       (when (= -1 (.indexOf actual# expected#))
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "to be in: " (-to-s actual#) " (using .contains)")))
       (and (speclj.platform/re? expected#) (string? actual#))
       (when (empty? (re-seq expected# actual#))
         (-fail (str "Expected: " (-to-s actual#) speclj.platform/endl "to match: " (-to-s expected#) " (using re-seq)")))
       (map? actual#)
       (when (not (contains? actual# expected#))
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "to be a key in: " (-to-s actual#) " (using contains?)")))
       (coll? actual#)
       (when (not (some #(= expected# %) actual#))
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "to be in: " (-to-s actual#) " (using =)")))
       :else (throw (speclj.platform/new-exception (str "should-contain doesn't know how to handle these types: [" (speclj.platform/type-name (type expected#)) " " (speclj.platform/type-name (type actual#)) "]"))))))

(defmacro should-not-contain
  "Multi-purpose assertion of non-containment.  See should-contain as an example of opposite behavior."
  [expected actual]
  `(let [expected# ~expected
         actual# ~actual]
     (cond
       (nil? actual#) nil ; automatic pass!
       (and (string? expected#) (string? actual#))
       (when (not (= -1 (.indexOf actual# expected#)))
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "not to be in: " (-to-s actual#) " (using .contains)")))
       (and (speclj.platform/re? expected#) (string? actual#))
       (when (not (empty? (re-seq expected# actual#)))
         (-fail (str "Expected: " (-to-s actual#) speclj.platform/endl "not to match: " (-to-s expected#) " (using re-seq)")))
       (map? actual#)
       (when (contains? actual# expected#)
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "not to be a key in: " (-to-s actual#) " (using contains?)")))
       (coll? actual#)
       (when (some #(= expected# %) actual#)
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "not to be in: " (-to-s actual#) " (using =)")))
       :else (throw (speclj.platform/new-exception (str "should-not-contain doesn't know how to handle these types: [" (speclj.platform/type-name (type expected#)) " " (speclj.platform/type-name (type actual#)) "]"))))))

(defmacro -remove-first [coll value]
  `(loop [coll# ~coll seen# []]
     (if (empty? coll#)
       seen#
       (let [f# (first coll#)]
         (if (= f# ~value)
           (concat seen# (rest coll#))
           (recur (rest coll#) (conj seen# f#)))))))

(defmacro -coll-difference [coll1 coll2]
  `(if (map? ~coll1)
     (first (clojure.data/diff ~coll1 ~coll2))
     (loop [match-with# ~coll1 match-against# ~coll2 diff# []]
       (if (empty? match-with#)
         diff#
         (let [f# (first match-with#)
               r# (rest match-with#)]
           (if (some #(= % f#) match-against#)
             (recur r# (-remove-first match-against# f#) diff#)
             (recur r# match-against# (conj diff# f#))))))))

(defmacro -difference-message [expected actual extra missing]
  `(str
     "Expected contents: " (-to-s ~expected) speclj.platform/endl
     "              got: " (-to-s ~actual) speclj.platform/endl
     "          missing: " (-to-s ~missing) speclj.platform/endl
     "            extra: " (-to-s ~extra)))

(defmacro should==
  "Asserts 'equivalency'.
  When passed collections it will check that they have the same contents.
  For anything else it will assert that clojure.core/== returns true."
  [expected actual]
  `(let [expected# ~expected
         actual# ~actual]
     (cond
       (and (coll? expected#) (coll? actual#))
       (let [extra# (-coll-difference actual# expected#)
             missing# (-coll-difference expected# actual#)]
         (when-not (and (empty? extra#) (empty? missing#))
           (-fail (-difference-message expected# actual# extra# missing#))))
       (and (number? expected#) (number? actual#))
       (when-not (== expected# actual#)
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "     got: " (-to-s actual#) " (using ==)")))
       :else (throw (speclj.platform/new-exception (str "should== doesn't know how to handle these types: " [(type expected#) (type actual#)]))))))

(defmacro should-not==
  "Asserts 'non-equivalency'.
  When passed collections it will check that they do NOT have the same contents.
  For anything else it will assert that clojure.core/== returns false."
  [expected actual]
  `(let [expected# ~expected
         actual# ~actual]
     (cond
       (and (coll? expected#) (coll? actual#))
       (let [extra# (-coll-difference actual# expected#)
             missing# (-coll-difference expected# actual#)]
         (when (and (empty? extra#) (empty? missing#))
           (-fail (str "Expected contents: " (-to-s expected#) speclj.platform/endl "   to differ from: " (-to-s actual#)))))
       (and (number? expected#) (number? actual#))
       (when-not (not (== expected# actual#))
         (-fail (str " Expected: " (-to-s expected#) speclj.platform/endl "not to ==: " (-to-s actual#) " (using ==)")))
       :else (throw (speclj.platform/new-exception (str "should-not== doesn't know how to handle these types: " [(type expected#) (type actual#)]))))))

(defmacro should-not-be-nil
  "Asserts that the form evaluates to a non-nil value"
  [form]
  `(should-not= nil ~form))

(defmacro should-fail
  "Forces a failure. An optional message may be passed in."
  ([] `(should-fail "Forced failure"))
  ([message] `(-fail ~message)))

(defmacro -create-should-throw-failure [expected actual expr]
  `(let [expected-name# (speclj.platform/type-name ~expected)
         expected-gaps# (apply str (repeat (count expected-name#) " "))
         actual-string# (if ~actual (pr-str ~actual) "<nothing thrown>")]
     (speclj.platform/new-failure (str "Expected " expected-name# " thrown from: " (pr-str ~expr) speclj.platform/endl
                                    "         " expected-gaps# "     but got: " actual-string#))))

(defmacro should-throw
  "Asserts that a Throwable is throws by the evaluation of a form.
When an class is passed, it assets that the thrown Exception is an instance of the class.
When a string is also passed, it asserts that the message of the Exception is equal to the string."
  ([form] `(should-throw speclj.platform/throwable ~form))
  ([throwable-type form]
    `(try
       ~form
       (throw (-create-should-throw-failure ~throwable-type nil '~form))
       (catch ~platform-macros/throwable e#
         (cond
           (speclj.platform/failure? e#) (throw e#)
           (not (isa? (type e#) ~throwable-type)) (throw (-create-should-throw-failure ~throwable-type e# '~form))
           :else e#))))
  ([throwable-type message form]
    `(let [e# (should-throw ~throwable-type ~form)]
       (try
         (should= ~message (speclj.platform/error-message e#))
         (catch ~platform-macros/throwable f# (-fail (str "Expected exception message didn't match" speclj.platform/endl (speclj.platform/error-message f#))))))))

(defmacro should-not-throw
  "Asserts that nothing is thrown by the evaluation of a form."
  [form]
  `(try
     ~form
     (catch ~platform-macros/throwable e# (-fail (str "Expected nothing thrown from: " (pr-str '~form) speclj.platform/endl
                                            "                     but got: " (pr-str e#))))))

(defmacro should-be-a
  "Asserts that the type of the given form derives from or equals the expected type"
  [expected-type actual-form]
  `(let [actual# ~actual-form
         actual-type# (type actual#)
         expected-type# ~expected-type]
     (when-not (isa? actual-type# expected-type#)
       (-fail (str "Expected " (-to-s actual#) " to be an instance of: " (-to-s expected-type#) speclj.platform/endl "           but was an instance of: " (-to-s actual-type#) " (using isa?)")))))

(defmacro should-not-be-a
  "Asserts that the type of the given form does not derives from or equals the expected type"
  [expected-type actual-form]
  `(let [actual# ~actual-form
         actual-type# (type actual#)
         expected-type# ~expected-type]
     (when (isa? actual-type# expected-type#)
       (-fail (str "Expected " (-to-s actual#) " not to be an instance of " (-to-s expected-type#) " but was (using isa?)")))))

(defmacro pending
  "When added to a characteristic, it is markes as pending.  If a message is provided it will be printed
  in the run report"
  ([] `(pending "Not Yet Implemented"))
  ([message]
    `(throw (platform-macros/new-pending ~message))))

(defmacro tags
  "Add tags to the containing context.  All values passed will be converted into keywords.  Contexts can be filtered
  at runtime by their tags.

  (tags :one :two)"
  [& values]
  (let [tag-kws (mapv keyword values)]
    `(mapv speclj.components/new-tag ~tag-kws)))

(defmacro with-stubs
  "Add this to describe/context blocks that use stubs.  It will setup a clean recording environment."
  []
  `(around [it#]
     (binding [speclj.stub/*stubbed-invocations* (atom [])]
       (it#))))

(defmacro stub
  "Creates a stub function.  Each call to the stub will be recorded an can later be looked up using the specified name.

  Options:
    :invoke - a function that will be invoked when the stub is invoked.  All the arguments passed to the stub will
      be passed to the :invoke value and it's return value returned by the stub.
    :return - a value that will be returned by the stub.  This overides the result of the :invoke value, if specified.
    :throw - an exception that will be thrown by the stub."
  ([name] `(speclj.stub/stub ~name {}))
  ([name options] `(speclj.stub/stub ~name ~options)))

(defmacro should-have-invoked
  "Checks for invocations of the specified stub.

  Options:
    :times - the number of times the stub should have been invoked. nil means at least once. (default: nil)
    :with - a list of arguments that the stubs should have been invoked with. Each call must have the same arguments.
      If not specified, anything goes.  Special expected arguments include:
       :* - matches anything
       a fn - matches when the actual is the same fn or calling fn with the actual argument returns true


  (should-have-invoked :foo {:with [1] :times 3})"
  ([name] `(should-have-invoked ~name {}))
  ([name options]
    `(let [name# ~name
           options# ~options
           invocations# (speclj.stub/invocations-of name#)
           times# (:times options#)
           check-params?# (contains? options# :with)
           with# (:with options#)
           with# (if (nil? with#) [] with#)]
       (if (number? times#)
         (when-not (= times# (count invocations#))
           (-fail (str "Expected: " times# " invocation" (if (= 1 times#) "" "s") " of " name# speclj.platform/endl "     got: " (count invocations#))))
         (when-not (seq invocations#)
           (-fail (str "Expected: an invocation of " name# speclj.platform/endl "     got: " (count invocations#)))))
       (when check-params?#
         (doseq [invocation# invocations#]
           (when-not (speclj.stub/params-match? with# invocation#)
             (-fail (str "Expected: invocation of " name# " with " (pr-str with#) speclj.platform/endl "     got: " (pr-str invocation#)))))))))

(defmacro should-not-have-invoked
  "Asserts that the specified stub was never invoked.

  Same as: (should-have-invoked :foo {:times 0})"
  ([name] `(should-not-have-invoked ~name {}))
  ([name options]
    `(should-have-invoked ~name ~(assoc options :times 0))))

(defmacro should-invoke
  "Creates a stub, and binds it to the specified var, evaluates the body, and checks the invocations.

  (should-invoke reverse {:with [1 2 3] :return []} (reverse [1 2 3]))

  See stub and should-have-invoked for valid options."
  [var options & body]
  (when-not (map? options)
    `(platform-macros/throw-error "The second argument to should-invoke must be a map of options"))
  (let [var-name (str var)]
    `(let [options# ~options]
       (binding [speclj.stub/*stubbed-invocations* (atom [])]
         (with-redefs [~var (speclj.stub/stub ~var-name options#)]
           ~@body)
         (should-have-invoked ~var-name options#)))))

(defmacro should-not-invoke
  "Creates a stub, and binds it to the specified var, evaluates the body, and checks that is was NOT invoked.

  Same as: (should-invoke :foo {:times 0} ...)"
  [var options & body]
  `(should-invoke ~var ~(assoc options :times 0) ~@body))

(defmacro run-specs []
  "If evaluated outsite the context of a spec run, it will run all the specs that have been evaulated using the default
runner and reporter.  A call to this function is typically placed at the end of a spec file so that all the specs
are evaluated by evaluation the file as a script.  Optional configuration paramters may be passed in:

(run-specs :stacktrace true :color false :reporter \"documentation\")"
  `(do
     (require '[speclj.cli]) ; require all speclj files
     (speclj.run.standard/run-specs)))


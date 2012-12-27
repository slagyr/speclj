(ns speclj.core
  (:use [speclj.running :only (submit-description run-and-report)]
        [speclj.reporting :only (report-message*)]
        [speclj.tags :only (describe-filter)]
        [speclj.config :only (active-reporters active-runner default-runner config-mappings default-config)]
        [speclj.components]
        [speclj.util :only (endl)])
  (:require [speclj.run.standard]
            [speclj.report.progress])
  (:import [speclj SpecFailure SpecPending]
           [java.util.regex Pattern]
           [clojure.lang IPersistentCollection IPersistentMap]))

(defmacro it
  "body => any forms but aught to contain at least one assertion (should)

  Declares a new characteristic (example in rspec)."
  [name & body]
  (if (seq body)
    `(new-characteristic ~name (fn [] ~@body))
    `(new-characteristic ~name (fn [] (pending)))))

(defmacro xit
  "Syntactic shortcut to make the characteristic pending."
  [name & body]
  `(it ~name (pending) ~@body))

(declare #^:dynamic *parent-description*)
(defmacro describe
  "body => & spec-components

  Declares a new spec.  The body can contain any forms that evaluate to spec compoenents (it, before, after, with ...)."
  [name & components]
  `(let [description# (new-description ~name *ns*)]
     (binding [*parent-description* description#]
       (doseq [component# (list ~@components)]
         (install component# description#)))
     (if (not (bound? #'*parent-description*))
       (submit-description (active-runner) description#))
     description#))

(defmacro context [name & components]
  `(describe ~name ~@components))

(defmacro before
  "Declares a function that is invoked before each characteristic in the containing describe scope is evaluated. The body
  may consist of any forms, presumably ones that perform side effects."
  [& body]
  `(new-before (fn [] ~@body)))

(defmacro after
  "Declares a function that is invoked after each characteristic in the containing describe scope is evaluated. The body
  may consist of any forms, presumably ones that perform side effects."
  [& body]
  `(new-after (fn [] ~@body)))

(defmacro around
  "Declares a function that will be invoked around each characteristic of the containing describe scope.
  The characteristic will be passed in and the around function is responsible for invoking it.

  (around [it] (binding [*out* new-out] (it)))
  "
  [binding & body]
  `(new-around (fn ~binding ~@body)))

(defmacro before-all
  "Declares a function that is invoked once before any characteristic in the containing describe scope is evaluated. The
  body may consist of any forms, presumably ones that perform side effects."
  [& body]
  `(new-before-all (fn [] ~@body)))

(defmacro after-all
  "Declares a function that is invoked once after all the characteristics in the containing describe scope have been
  evaluated.  The body may consist of any forms, presumably ones that perform side effects."
  [& body]
  `(new-after-all (fn [] ~@body)))

(defmacro with
  "Declares a reference-able symbol that will be lazily evaluated once per characteristic of the containing
  describe scope.  The body may contain any forms, the last of which will be the value of the dereferenced symbol.

  (with meaning 42)
  (it \"knows the meaining life\" (should= @meaning (the-meaning-of :life)))"
  [name & body]
  (let [var-name (with-meta (symbol name) {:dynamic true})]
    `(do
       (let [with-component# (new-with '~var-name (fn [] ~@body))]
         (declare ~var-name)
         with-component#))))

(defmacro with-all
  "Declares a reference-able symbol that will be lazily evaluated once per context. The body may contain any forms,
   the last of which will be the value of the dereferenced symbol.

  (with-all meaning 42)
  (it \"knows the meaining life\" (should= @meaning (the-meaning-of :life)))"
  [name & body]
  (let [var-name (with-meta (symbol name) {:dynamic true})]
    `(do
       (let [with-all-component# (new-with-all '~var-name (fn [] ~@body))]
         (declare ~var-name)
         with-all-component#))))

(defn -to-s [thing]
  (if (nil? thing) "nil" (str "<" (pr-str thing) ">")))

(defmacro should
  "Asserts the truthy-ness of a form"
  [form]
  `(let [value# ~form]
     (if-not value#
       (throw (SpecFailure. (str "Expected truthy but was: " (-to-s value#) ""))))))

(defmacro should-not
  "Asserts the falsy-ness of a form"
  [form]
  `(let [value# ~form]
     (if value#
       (throw (SpecFailure. (str "Expected falsy but was: " (-to-s value#)))))))

(defmacro should=
  "Asserts that two forms evaluate to equal values, with the expexcted value as the first parameter."
  ([expected-form actual-form]
    `(let [expected# ~expected-form actual# ~actual-form]
       (if (not (= expected# actual#))
         (throw (SpecFailure. (str "Expected: " (-to-s expected#) endl "     got: " (-to-s actual#) " (using =)"))))))
  ([expected-form actual-form delta-form]
    `(let [expected# ~expected-form actual# ~actual-form delta# ~delta-form]
       (if (> (.abs (- (bigdec expected#) (bigdec actual#))) (.abs (bigdec delta#)))
         (throw (SpecFailure. (str "Expected: " (-to-s expected#) endl "     got: " (-to-s actual#) " (using delta: " delta# ")")))))))

(defmacro should-not=
  "Asserts that two forms evaluate to inequal values, with the unexpexcted value as the first parameter."
  [expected-form actual-form]
  `(let [expected# ~expected-form actual# ~actual-form]
     (if (= expected# actual#)
       (throw (SpecFailure. (str "Expected: " (-to-s expected#) endl "not to =: " (-to-s actual#)))))))

(defmacro should-be-same
  "Asserts that two forms evaluate to the same object, with the expexcted value as the first parameter."
  [expected-form actual-form]
  `(let [expected# ~expected-form actual# ~actual-form]
     (if (not (identical? expected# actual#))
       (throw (SpecFailure. (str "         Expected: " (-to-s expected#) endl "to be the same as: " (-to-s actual#) " (using identical?)"))))))

(defmacro should-not-be-same
  "Asserts that two forms evaluate to different objects, with the unexpexcted value as the first parameter."
  [expected-form actual-form]
  `(let [expected# ~expected-form actual# ~actual-form]
     (if (identical? expected# actual#)
       (throw (SpecFailure. (str "             Expected: " (-to-s expected#) endl "not to be the same as: " (-to-s actual#) " (using identical?)"))))))

(defmacro should-be-nil
  "Asserts that the form evaluates to nil"
  [form]
  `(should= nil ~form))

(defmulti -should-contain (fn [expected actual] [(type expected) (type actual)]))
(defmulti -should-not-contain (fn [expected actual] [(type expected) (type actual)]))

(defmethod -should-contain [String String] [seeking within]
  (when (not (.contains within seeking))
    (throw (SpecFailure. (str "Expected: " (-to-s seeking) endl "to be in: " (-to-s within) " (using .contains)")))))

(defmethod -should-not-contain [String String] [seeking within]
  (when (.contains within seeking)
    (throw (SpecFailure. (str "Expected: " (-to-s seeking) endl "not to be in: " (-to-s within) " (using .contains)")))))

(defmethod -should-contain [Pattern String] [pattern actual]
  (when (empty? (re-seq pattern actual))
    (throw (SpecFailure. (str "Expected: " (-to-s actual) endl "to match: " (-to-s pattern) " (using re-seq)")))))

(defmethod -should-not-contain [Pattern String] [pattern actual]
  (when (not (empty? (re-seq pattern actual)))
    (throw (SpecFailure. (str "Expected: " (-to-s actual) endl "not to match: " (-to-s pattern) " (using re-seq)")))))

(defmethod -should-contain [Object IPersistentMap] [element coll]
  (when (not (contains? coll element))
    (throw (SpecFailure. (str "Expected: " (-to-s element) endl "to be a key in: " (-to-s coll) " (using contains?)")))))

(defmethod -should-not-contain [Object IPersistentMap] [element coll]
  (when (contains? coll element)
    (throw (SpecFailure. (str "Expected: " (-to-s element) endl "not to be a key in: " (-to-s coll) " (using contains?)")))))

(defmethod -should-contain [Object IPersistentCollection] [element coll]
  (when (not (some #(= element %) coll))
    (throw (SpecFailure. (str "Expected: " (-to-s element) endl "to be in: " (-to-s coll) " (using =)")))))

(defmethod -should-not-contain [Object IPersistentCollection] [element coll]
  (when (some #(= element %) coll)
    (throw (SpecFailure. (str "Expected: " (-to-s element) endl "not to be in: " (-to-s coll) " (using =)")))))

(defmethod -should-contain :default [expected actual]
  (throw (Exception. (str "should-contain doesn't know how to handle these types: " [(type expected) (type actual)]))))

(defmethod -should-not-contain :default [expected actual]
  (throw (Exception. (str "should-not-contain doesn't know how to handle these types: " [(type expected) (type actual)]))))

(defmacro should-contain
  "Multi-purpose assertion of containment.  Works strings, regular expressions, sequences, and maps.

  (should-contain \"foo\" \"foobar\")            ; looks for sub-string
  (should-contain #\"hello.*\" \"hello, world\") ; looks for regular expression
  (should-contain :foo {:foo :bar})          ; looks for a key in a map
  (should-contain 3 [1 2 3 4])               ; looks for an object in a collection"
  [expected actual]
  `(-should-contain ~expected ~actual))

(defmacro should-not-contain
  "Multi-purpose assertion of non-containment.  See should-contain as an example of opposite behavior."
  [expected actual]
  `(-should-not-contain ~expected ~actual))

(defn coll-includes? [coll item]
  (some #(= % item) coll))

(defn remove-first [coll value]
  (loop [coll coll seen []]
    (if (empty? coll)
      seen
      (let [f (first coll)]
        (if (= f value)
          (concat seen (rest coll))
          (recur (rest coll) (conj seen f)))))))

(defn coll-difference [coll1 coll2]
  (loop [match-with coll1 match-against coll2 diff []]
    (if (empty? match-with)
      diff
      (let [f (first match-with)
            r (rest match-with)]
        (if (coll-includes? match-against f)
          (recur r (remove-first match-against f) diff)
          (recur r match-against (conj diff f)))))))

(defn difference-message [expected actual extra missing]
  (str
    "Expected contents: " (-to-s expected) endl
    "              got: " (-to-s actual) endl
    "          missing: " (-to-s missing) endl
    "            extra: " (-to-s extra)))

(defmulti -should== (fn [expected actual] [(type expected) (type actual)]))
(defmulti -should-not== (fn [expected actual] [(type expected) (type actual)]))

(defmethod -should== [Object Object] [expected actual]
  (when-not (== expected actual)
    (throw (SpecFailure. (str "Expected: " (-to-s expected) endl "     got: " (-to-s actual) " (using ==)")))))

(defmethod -should-not== [Object Object] [expected actual]
  (when-not (not (== expected actual))
    (throw (SpecFailure. (str " Expected: " (-to-s expected) endl "not to ==: " (-to-s actual) " (using ==)")))))

(defmethod -should== [IPersistentCollection IPersistentCollection] [expected actual]
  (let [extra (coll-difference actual expected)
        missing (coll-difference expected actual)]
    (when-not (and (empty? extra) (empty? missing))
      (throw (SpecFailure. (difference-message expected actual extra missing))))))

(defmethod -should-not== [IPersistentCollection IPersistentCollection] [expected actual]
  (let [extra (coll-difference actual expected)
        missing (coll-difference expected actual)]
    (when (and (empty? extra) (empty? missing))
      (throw (SpecFailure. (str "Expected contents: " (-to-s expected) endl "   to differ from: " (-to-s actual)))))))

(defmacro should==
  "Asserts 'equivalency'.
  When passed collections it will check that they have the same contents.
  For anything else it will assert that clojure.core/== returns true."
  [expected actual]
  `(-should== ~expected ~actual))

(defmacro should-not==
  "Asserts 'non-equivalency'.
  When passed collections it will check that they do NOT have the same contents.
  For anything else it will assert that clojure.core/== returns false."
  [expected actual]
  `(-should-not== ~expected ~actual))

(defmacro should-not-be-nil
  "Asserts that the form evaluates to a non-nil value"
  [form]
  `(should-not= nil ~form))

(defmacro should-fail
  "Forces a failure. An optional message may be passed in."
  ([] `(should-fail "Forced failure"))
  ([message] `(throw (SpecFailure. ~message))))

(defmacro -create-should-throw-failure [expected actual expr]
  `(let [expected-name# (.getName ~expected)
         expected-gaps# (apply str (repeat (count expected-name#) " "))
         actual-string# (if ~actual (.toString ~actual) "<nothing thrown>")]
     (SpecFailure. (str "Expected " expected-name# " thrown from: " ~expr endl
                     "         " expected-gaps# "     but got: " actual-string#))))

(defmacro should-throw
  "Asserts that a Throwable is throws by the evaluation of a form.
When an class is passed, it assets that the thrown Exception is an instance of the class.
When a string is also passed, it asserts that the message of the Exception is equal to the string."
  ([form] `(should-throw Throwable ~form))
  ([throwable-type form]
    `(try
       ~form
       (throw (-create-should-throw-failure ~throwable-type nil '~form))
       (catch Throwable e#
         (cond
           (.isInstance SpecFailure e#) (throw e#)
           (not (.isInstance ~throwable-type e#)) (throw (-create-should-throw-failure ~throwable-type e# '~form))
           :else e#))))
  ([throwable-type message form]
    `(let [e# (should-throw ~throwable-type ~form)]
       (try
         (should= ~message (.getMessage e#))
         (catch SpecFailure f# (throw (SpecFailure. (str "Expected exception message didn't match" endl (.getMessage f#)))))))))

(defmacro should-not-throw
  "Asserts that nothing is thrown by the evaluation of a form."
  [form]
  `(try
     ~form
     (catch Throwable e# (throw (SpecFailure. (str "Expected nothing thrown from: " '~form endl
                                                "                     but got: " (.toString e#)))))))

(defmacro should-be-a
  "Asserts that the type of the given form derives from or equals the expected type"
  [expected-type actual-form]
  `(let [actual# ~actual-form
         actual-type# (type actual#)
         expected-type# ~expected-type]
     (if-not (isa? actual-type# expected-type#)
       (throw
         (SpecFailure.
           (str "Expected " (-to-s actual#) " to be an instance of: " (-to-s expected-type#) endl "           but was an instance of: " (-to-s actual-type#) " (using isa?)"))))))

(defmacro should-not-be-a
  "Asserts that the type of the given form does not derives from or equals the expected type"
  [expected-type actual-form]
  `(let [actual# ~actual-form
         actual-type# (type actual#)
         expected-type# ~expected-type]
     (if (isa? actual-type# expected-type#)
       (throw
         (SpecFailure.
           (str "Expected " (-to-s actual#) " not to be an instance of " (-to-s expected-type#) " but was (using isa?)"))))))

(defmacro pending
  "When added to a characteristic, it is markes as pending.  If a message is provided it will be printed
  in the run report"
  ([] `(pending "Not Yet Implemented"))
  ([message]
    `(throw (SpecPending. ~message))))

(defn tags
  "Add tags to the containing context.  All values passed will be converted into keywords.  Contexts can be filtered
  at runtime by their tags.

  (tags :one :two)"
  [& values]
  (let [tag-kws (map keyword values)]
    (map new-tag tag-kws)))

(defn run-specs [& configurations]
  "If evaluated outsite the context of a spec run, it will run all the specs that have been evaulated using the default
runner and reporter.  A call to this function is typically placed at the end of a spec file so that all the specs
are evaluated by evaluation the file as a script.  Optional configuration paramters may be passed in:

(run-specs :stacktrace true :color false :reporter \"documentation\")"
  (when (identical? (active-runner) @default-runner) ; Solo file run?
    (let [config (apply hash-map configurations)
          config (merge (dissoc default-config :runner ) config)]
      (with-bindings (config-mappings config)
        (if-let [filter-msg (describe-filter)]
          (report-message* (active-reporters) filter-msg))
        (run-and-report (active-runner) (active-reporters))))))

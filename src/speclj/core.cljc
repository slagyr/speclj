(ns speclj.core
  "Speclj's API. It contains nothing but macros, so that it can be used
  in both Clojure and ClojureScript."
  (:require [clojure.data]
            [clojure.string]
            [speclj.components]
            [speclj.config]
            #?(:clj  [speclj.platform :refer [if-cljs try-catch-anything]]
               :cljs [speclj.platform])
            [speclj.reporting]
            [speclj.results]
            [speclj.running]
            [speclj.stub]
            [speclj.tags]
            [speclj.report.documentation]
            [speclj.report.progress]
            [speclj.report.silent]
            [speclj.run.standard]))

#?(:clj (try (require 'speclj.run.standard)
             (catch Exception _)))

(defmacro ^:no-doc -new-exception
  ([] `(if-cljs (js/Error.) (java.lang.Exception.)))
  ([message] `(if-cljs (js/Error. ~message) (java.lang.Exception. ~message)))
  ([message cause] `(if-cljs (js/Error. ~message) (java.lang.Exception. ~message ~cause))))

(defmacro ^:no-doc -new-throwable
  ([] `(if-cljs (js/Object.) (java.lang.Throwable.)))
  ([message] `(if-cljs (js/Object. ~message) (java.lang.Throwable. ~message))))

(defmacro ^:no-doc -new-failure [message]
  `(speclj.platform.SpecFailure. ~message))

(defmacro ^:no-doc -new-pending [message]
  `(speclj.platform.SpecPending. ~message))

(defmacro ^:no-doc help-it [name focused? & body]
  (if (seq body)
    `(speclj.components/new-characteristic ~name (fn [] ~@body) ~focused?)
    `(speclj.components/new-characteristic ~name (fn [] (pending)) ~focused?)))

(defmacro ^:no-doc help-describe [name focused? & components]
  `(let [description# (speclj.components/new-description ~name ~focused? ~(clojure.core/name (.name *ns*)))]
     (binding [speclj.config/*parent-description* description#]
       ; MDM - use a vector below - cljs generates a warning because def/declares don't eval immediately
       (doseq [component# (vector ~@components)]
         (speclj.components/install component# description#)))
     (when-not (if-cljs
                 speclj.config/*parent-description*
                 (bound? #'speclj.config/*parent-description*))
       (speclj.running/submit-description (speclj.config/active-runner) description#))
     description#))

(defmacro it
  "body => any forms, but should contain at least one assertion (should)

  Declares a new characteristic (example in rspec)."
  [name & body]
  `(help-it ~name false ~@body))

(defmacro xit
  "Syntactic shortcut to make the characteristic pending."
  [name & body]
  `(it ~name (pending) ~@body))

(defmacro focus-it
  "Same as 'it', but it is meant to facilitate temporary debugging.
  Characteristics defined with this macro will be executed along with any
  other characteristics thus defined, but all other characteristic defined
  with 'it' will be ignored."
  [name & body]
  `(help-it ~name true ~@body))

(defmacro ^:no-doc when-not-bound [name & body]
  `(if-cljs
     (when-not ~name ~@body)
     (when-not (bound? (find-var '~name)) ~@body)))

(defmacro describe
  "body => & spec-components

  Declares a new spec.  The body can contain any forms that evaluate to spec components (it, before, after, with ...)."
  [name & components]
  `(help-describe ~name false ~@components))

(defmacro focus-describe
  "Same as 'describe', but it is meant to facilitate temporary debugging.
   Components defined with this macro will be fully executed along with any
   other components thus defined, but all other sibling components defined
   with 'describe' will be ignored."
  [name & components]
  `(help-describe ~name true ~@components))

(defmacro context
  "Same as describe, but should be used to nest testing contexts inside the outer describe.
  Contexts can be nested any number of times."
  [name & components]
  `(describe ~name ~@components))

(defmacro focus-context
  "Same as 'context', but it is meant to facilitate temporary debugging.
   Components defined with this macro will be fully executed along with any
   other components thus defined, but all other sibling components defined
   with 'context' will be ignored."
  [name & components]
  `(focus-describe ~name ~@components))

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

  Note that if you have cleanup that must run, use a 'finally' block:

  (around [it] (try (it) (finally :clean-up)))"
  [binding & body]
  `(speclj.components/new-around (fn ~binding ~@body)))

(defmacro redefs-around
  "Redefines the bindings around each characteristic of the containing describe scope."
  [bindings]
  `(around [it#] (with-redefs ~bindings (it#))))

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

(defmacro around-all
  "Declares a function that is invoked once around all characteristics of the containing describe scope."
  [context & body]
  `(speclj.components/new-around-all (fn ~context ~@body)))

(defn ^:no-doc -make-with [name body ctor bang?]
  (let [var-name (with-meta (symbol name) {:dynamic true})]
    `(do
       (declare ~var-name)
       (~ctor '~var-name (fn [] ~@body) (fn [v#] (set! ~var-name v#)) ~bang?))))

(defmacro with
  "Declares a reference-able symbol that will be lazily evaluated once per characteristic of the containing
  describe scope.  The body may contain any forms, the last of which will be the value of the dereferenced symbol.

  (with meaning 42)
  (it \"knows the meaning of life\" (should= @meaning (the-meaning-of :life)))"
  [name & body]
  (-make-with name body `speclj.components/new-with false))

(defmacro with!
  "Declares a reference-able symbol that will be evaluated immediately and reset once per characteristic of the containing
  describe scope.  The body may contain any forms, the last of which will be the value of the dereferenced symbol.

  (def my-num (atom 0))
  (with! my-with! (swap! my-num inc))
  (it \"increments my-num before being accessed\" (should= 1 @my-num) (should= 2 @my-with!))"
  [name & body]
  (-make-with name body `speclj.components/new-with true))

(defmacro with-all
  "Declares a reference-able symbol that will be lazily evaluated once per context. The body may contain any forms,
   the last of which will be the value of the dereferenced symbol.

  (with-all meaning 42)
  (it \"knows the meaning of life\" (should= @meaning (the-meaning-of :life)))"
  [name & body]
  (-make-with name body `speclj.components/new-with-all false))

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
  (-make-with name body `speclj.components/new-with-all true))

(defmacro ^:no-doc -to-s [thing]
  `(if-some [thing# ~thing] (pr-str thing#) "nil"))

(defmacro -fail
  "Useful for making custom assertions."
  [message]
  `(throw (-new-failure ~message)))

(defmacro ^:no-doc wrong-types [assertion a b]
  `(let [a#      ~a
         b#      ~b
         type-a# (if (nil? a#) "nil" (speclj.platform/type-name (type a#)))
         type-b# (if (nil? b#) "nil" (speclj.platform/type-name (type b#)))]
     (str ~assertion " doesn't know how to handle these types: [" type-a# " " type-b# "]")))

(defmacro should
  "Asserts the truthy-ness of a form"
  [form]
  `(let [value# ~form]
     (when-not value#
       (-fail (str "Expected truthy but was: " (-to-s value#) "")))))

(defmacro should-not
  "Asserts the falsy-ness of a form"
  [form]
  `(when-let [value# ~form]
     (-fail (str "Expected falsy but was: " (-to-s value#)))))

(defmacro should=
  "Asserts that two forms evaluate to equal values, with the expected value as the first parameter."
  ([expected-form actual-form]
   `(let [expected# ~expected-form actual# ~actual-form]
      (when-not (= expected# actual#)
        (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "     got: " (-to-s actual#) " (using =)")))))
  ([expected-form actual-form delta-form]
   `(let [expected# ~expected-form actual# ~actual-form delta# ~delta-form]
      (when (speclj.platform/difference-greater-than-delta? expected# actual# delta#)
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
  "Asserts that two forms evaluate to unequal values, with the unexpected value as the first parameter."
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
  "Multipurpose assertion of containment.  Works on strings, regular expressions, sequences, and maps.

  (should-contain \"foo\" \"foobar\")            ; looks for sub-string
  (should-contain #\"hello.*\" \"hello, world\") ; looks for regular expression
  (should-contain :foo {:foo :bar})          ; looks for a key in a map
  (should-contain 3 [1 2 3 4])               ; looks for an object in a collection"
  [expected actual]
  `(let [expected# ~expected
         actual#   ~actual]
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
       :else (throw (-new-exception (wrong-types "should-contain" expected# actual#))))))

(defmacro should-not-contain
  "Multipurpose assertion of non-containment.  See should-contain as an example of opposite behavior."
  [expected actual]
  `(let [expected# ~expected
         actual#   ~actual]
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
       :else (throw (-new-exception (wrong-types "should-not-contain" expected# actual#))))))

(defmacro should-have-count
  "Multipurpose assertion on (count %). Works on strings, sequences, and maps.

  (should-have-count 6 \"foobar\")
  (should-have-count 2 [1 2])
  (should-have-count 1 {:foo :bar})
  (should-have-count 0 [])
  (should-have-count 0 nil)"
  [expected coll]
  `(let [expected# ~expected
         coll#     ~coll]
     (if-not (and (number? expected#) (or (nil? coll#) (string? coll#) (counted? coll#)))
       (throw (-new-exception (wrong-types "should-have-count" expected# coll#)))
       (let [actual# (count coll#)]
         (when-not (= expected# actual#)
           (-fail (str "Expected count: " expected# speclj.platform/endl
                       "Actual count:   " actual# speclj.platform/endl
                       "Actual coll:    " (-to-s coll#))))))))

(defmacro should-not-have-count
  "Multipurpose assertion on (not= (count %)). Works on strings, sequences, and maps.

  (should-not-have-count 1 \"foobar\")
  (should-not-have-count 1 [1 2])
  (should-not-have-count 42 {:foo :bar})
  (should-not-have-count 1 [])
  (should-not-have-count 1 nil)"
  [expected coll]
  `(let [expected# ~expected
         coll#     ~coll]
     (if-not (and (number? expected#) (or (nil? coll#) (string? coll#) (counted? coll#)))
       (throw (-new-exception (wrong-types "should-not-have-count" expected# coll#)))
       (let [actual# (count coll#)]
         (when (= expected# actual#)
           (-fail (str "Expected count to not equal " expected# " (but it did!)" speclj.platform/endl
                       "Collection: " (-to-s coll#))))))))

(defmacro ^:no-doc -remove-first [coll value]
  `(let [value# ~value]
     (loop [coll# ~coll seen# []]
       (if (empty? coll#)
         seen#
         (let [f# (first coll#)]
           (if (= f# value#)
             (concat seen# (rest coll#))
             (recur (rest coll#) (conj seen# f#))))))))

(defmacro ^:no-doc -coll-difference [coll1 coll2]
  `(let [coll1# ~coll1
         coll2# ~coll2]
     (if (map? coll1#)
       (first (clojure.data/diff coll1# coll2#))
       (loop [match-with# coll1# match-against# coll2# diff# []]
         (if (empty? match-with#)
           diff#
           (let [f# (first match-with#)
                 r# (rest match-with#)]
             (if (some #(= % f#) match-against#)
               (recur r# (-remove-first match-against# f#) diff#)
               (recur r# match-against# (conj diff# f#)))))))))

(defmacro should-start-with
  "Assertion of prefix in strings and sequences.

  (should-start-with \"foo\" \"foobar\")            ; looks for string prefix
  (should-start-with [1 2] [1 2 3 4])               ; looks for a subset at start of collection"
  [prefix whole]
  `(let [prefix# ~prefix
         whole#  ~whole]
     (cond
       (and (string? prefix#) (string? whole#))
       (when-not (clojure.string/starts-with? whole# prefix#)
         (-fail (str "Expected \"" whole# "\" to start" speclj.platform/endl
                     "    with \"" prefix# "\"")))

       (and (coll? whole#) (coll? prefix#))
       (let [actual#  (take (count prefix#) whole#)
             extra#   (-coll-difference actual# prefix#)
             missing# (-coll-difference prefix# actual#)]
         (when-not (and (empty? extra#) (empty? missing#))
           (-fail (str "Expected " (-to-s whole#) " to start" speclj.platform/endl
                       "    with " (-to-s prefix#)))))

       :else
       (throw (-new-exception (wrong-types "should-start-with" prefix# whole#))))))

(defmacro should-not-start-with
  "The inverse of should-start-with."
  [prefix whole]
  `(let [prefix# ~prefix
         whole#  ~whole]
     (cond
       (and (string? prefix#) (string? whole#))
       (when (clojure.string/starts-with? whole# prefix#)
         (-fail (str "Expected \"" whole# "\" to NOT start" speclj.platform/endl
                     "    with \"" prefix# "\"")))

       (and (coll? whole#) (coll? prefix#))
       (let [actual#  (take (count prefix#) whole#)
             extra#   (-coll-difference actual# prefix#)
             missing# (-coll-difference prefix# actual#)]
         (when (and (empty? extra#) (empty? missing#))
           (-fail (str "Expected " (-to-s whole#) " to NOT start" speclj.platform/endl
                       "    with " (-to-s prefix#)))))

       :else (throw (-new-exception (wrong-types "should-not-start-with" prefix# whole#))))))

(defmacro should-end-with
  "Assertion of suffix in strings and sequences.

  (should-end-with \"foo\" \"foobar\")            ; looks for string suffix
  (should-end-with [1 2] [1 2 3 4])               ; looks for a subset at end of collection"
  [suffix whole]
  `(let [suffix# ~suffix
         whole#  ~whole]
     (cond
       (and (string? suffix#) (string? whole#))
       (when-not (clojure.string/ends-with? whole# suffix#)
         (let [padding# (apply str (repeat (- (count whole#) (count suffix#)) " "))]
           (-fail (str "Expected [" whole# "] to end\n" padding#
                       "    with [" suffix# "]"))))

       (and (coll? whole#) (coll? suffix#))
       (let [actual#  (take-last (count suffix#) whole#)
             extra#   (-coll-difference actual# suffix#)
             missing# (-coll-difference suffix# actual#)]
         (when-not (and (empty? extra#) (empty? missing#))
           (let [whole#   (-to-s whole#)
                 suffix#  (-to-s suffix#)
                 padding# (apply str (repeat (- (count whole#) (count suffix#)) " "))]
             (-fail (str "Expected " whole# " to end\n" padding#
                         "    with " suffix#)))))

       :else
       (throw (-new-exception (wrong-types "should-end-with" suffix# whole#))))))

(defmacro should-not-end-with
  "The inverse of should-end-with."
  [prefix whole]
  `(let [prefix# ~prefix
         whole#  ~whole]
     (cond
       (and (string? prefix#) (string? whole#))
       (when (clojure.string/ends-with? whole# prefix#)
         (let [padding# (apply str (repeat (- (count whole#) (count prefix#)) " "))]
           (-fail (str "Expected [" whole# "] to NOT end\n" padding#
                       "    with [" prefix# "]"))))

       (and (coll? whole#) (coll? prefix#))
       (let [actual#  (take-last (count prefix#) whole#)
             extra#   (-coll-difference actual# prefix#)
             missing# (-coll-difference prefix# actual#)]
         (when (and (empty? extra#) (empty? missing#))
           (let [whole#   (-to-s whole#)
                 prefix#  (-to-s prefix#)
                 padding# (apply str (repeat (- (count whole#) (count prefix#)) " "))]
             (-fail (str "Expected " whole# " to NOT end\n" padding#
                         "    with " prefix#)))))

       :else (throw (-new-exception (wrong-types "should-not-end-with" prefix# whole#))))))

(defmacro ^:no-doc -difference-message [expected actual extra missing]
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
         actual#   ~actual]
     (cond
       (and (coll? expected#) (coll? actual#))
       (let [extra#   (-coll-difference actual# expected#)
             missing# (-coll-difference expected# actual#)]
         (when-not (and (empty? extra#) (empty? missing#))
           (-fail (-difference-message expected# actual# extra# missing#))))
       (and (number? expected#) (number? actual#))
       (when-not (== expected# actual#)
         (-fail (str "Expected: " (-to-s expected#) speclj.platform/endl "     got: " (-to-s actual#) " (using ==)")))
       :else (throw (-new-exception (wrong-types "should==" expected# actual#))))))

(defmacro should-not==
  "Asserts 'non-equivalency'.
  When passed collections it will check that they do NOT have the same contents.
  For anything else it will assert that clojure.core/== returns false."
  [expected actual]
  `(let [expected# ~expected
         actual#   ~actual]
     (cond
       (and (coll? expected#) (coll? actual#))
       (let [extra#   (-coll-difference actual# expected#)
             missing# (-coll-difference expected# actual#)]
         (when (and (empty? extra#) (empty? missing#))
           (-fail (str "Expected contents: " (-to-s expected#) speclj.platform/endl "   to differ from: " (-to-s actual#)))))
       (and (number? expected#) (number? actual#))
       (when-not (not (== expected# actual#))
         (-fail (str " Expected: " (-to-s expected#) speclj.platform/endl "not to ==: " (-to-s actual#) " (using ==)")))
       :else (throw (-new-exception (wrong-types "should-not==" expected# actual#))))))

(defmacro should-not-be-nil
  "Asserts that the form evaluates to a non-nil value"
  [form]
  `(should-not= nil ~form))

(defmacro should-fail
  "Forces a failure. An optional message may be passed in."
  ([] `(should-fail "Forced failure"))
  ([message] `(-fail ~message)))

(defmacro ^:no-doc -create-should-throw-failure [expected actual expr]
  `(let [expected-name# (speclj.platform/type-name ~expected)
         expected-gaps# (apply str (repeat (count expected-name#) " "))
         actual-string# (if-let [actual# ~actual] (pr-str actual#) "<nothing thrown>")]
     (-new-failure (str "Expected " expected-name# " thrown from: " (pr-str ~expr) speclj.platform/endl
                        "         " expected-gaps# "     but got: " actual-string#))))

(defmacro should-throw
  "Asserts that a Throwable is throws by the evaluation of a form.
When a class is passed, it asserts that the thrown Exception is an instance of the class.
There are three options for passing different kinds of predicates:
  - If a string, assert that the message of the Exception is equal to the string.
  - If a regex, asserts that the message of the Exception matches the regex.
  - If a function, assert that calling the function on the Exception returns a truthy value."
  ([form] `(should-throw speclj.platform/throwable ~form))
  ([throwable-type form]
   `(try-catch-anything
      ~form
      (throw (-create-should-throw-failure ~throwable-type nil '~form))
      (catch e#
             (cond
               (speclj.platform/failure? e#) (throw e#)
               (not (instance? ~throwable-type e#)) (throw (-create-should-throw-failure ~throwable-type e# '~form))
               :else e#))))
  ([throwable-type predicate form]
   `(let [e#     (should-throw ~throwable-type ~form)
          regex# (if-cljs js/RegExp java.util.regex.Pattern)]
      (try-catch-anything
        (let [predicate# ~predicate]
          (cond (instance? regex# predicate#)
                (should-not-be-nil (re-find predicate# (speclj.platform/error-message e#)))

                (ifn? predicate#)
                (should= true (predicate# e#))

                :else
                (should= predicate# (speclj.platform/error-message e#))))

        (catch f# (-fail (str "Expected exception predicate didn't match" speclj.platform/endl (speclj.platform/error-message f#))))))))

(defmacro should-not-throw
  "Asserts that nothing is thrown by the evaluation of a form."
  [form]
  `(try-catch-anything
     ~form
     (catch e#
            (-fail (str "Expected nothing thrown from: " (pr-str '~form) speclj.platform/endl
                        "                     but got: " (pr-str e#))))))

(defmacro should-be-a
  "Asserts that the type of the given form derives from or equals the expected type"
  [expected-type actual-form]
  `(let [actual#        ~actual-form
         actual-type#   (type actual#)
         expected-type# ~expected-type]
     (when-not (isa? actual-type# expected-type#)
       (-fail (str "Expected " (-to-s actual#) " to be an instance of: " (-to-s expected-type#) speclj.platform/endl "           but was an instance of: " (-to-s actual-type#) " (using isa?)")))))

(defmacro should-not-be-a
  "Asserts that the type of the given form does not derive from or equal the expected type"
  [expected-type actual-form]
  `(let [actual#        ~actual-form
         actual-type#   (type actual#)
         expected-type# ~expected-type]
     (when (isa? actual-type# expected-type#)
       (-fail (str "Expected " (-to-s actual#) " not to be an instance of " (-to-s expected-type#) " but was (using isa?)")))))

(defmacro pending
  "When added to a characteristic, it is marked as pending.  If a message is provided it will be printed
  in the run report"
  ([] `(pending "Not Yet Implemented"))
  ([message]
   `(throw (-new-pending ~message))))

(defmacro tags
  "Add tags to the containing context.  All values passed will be converted into keywords.  Contexts can be filtered
  at runtime by their tags.

  (tags :one :two)"
  [& values]
  (let [tag-kws (mapv keyword values)]
    `(mapv speclj.components/new-tag ~tag-kws)))

(defmacro with-stubs
  "Add this to describe/context blocks that use stubs.  It will set up a clean recording environment."
  []
  `(around [it#]
     (with-redefs [speclj.stub/*stubbed-invocations* (atom [])]
       (it#))))

(defmacro stub
  "Creates a stub function.  Each call to the stub will be recorded and can later be looked up using the specified name.

  Options:
    :invoke - a function that will be invoked when the stub is invoked.  All the arguments passed to the stub will
      be passed to the :invoke value and its return value returned by the stub.
    :return - a value that will be returned by the stub.  This overrides the result of the :invoke value, if specified.
    :throw - an exception that will be thrown by the stub."
  ([name] `(speclj.stub/stub ~name {}))
  ([name options] `(speclj.stub/stub ~name ~options)))

(defmacro should-have-invoked
  "Checks for invocations of the specified stub.

  Options:
    :times - the number of times the stub should have been invoked. nil means at least once. (default: nil)
    :with - a list of arguments that the stubs should have been invoked with.
      If not specified, anything goes.  Special expected arguments include:
       :* - matches anything
       a fn - matches when the actual is the same fn or calling fn with the actual argument returns true

  Example:
  (let [foo (stub :foo)]
    (should-have-invoked :foo {:with [1] :times 3}) ; fail
    (foo 1)
    (foo 2)
    (should-have-invoked :foo {:with [1] :times 3}) ; fail
    (should-have-invoked :foo {:with [1] :times 1}) ; pass
    (should-have-invoked :foo {:with [2] :times 1}) ; pass
    (should-have-invoked :foo {:times 3}) ; fail
    (should-have-invoked :foo {:times 2}) ; pass
    (should-have-invoked :foo {:times 1}) ; fail
    (should-have-invoked :foo {:with [1]}) ; pass
    (should-have-invoked :foo {:with [2]}) ; pass
    )"
  ([name] `(should-have-invoked ~name {}))
  ([name options]
   `(let [name#            ~name
          options#         ~options
          invocations#     (speclj.stub/invocations-of name#)
          times#           (:times options#)
          times?#          (number? times#)
          check-params?#   (contains? options# :with)
          with#            (:with options#)
          with#            (if (nil? with#) [] with#)
          invocations-str# #(if (= 1 %) "invocation" "invocations")]
      (cond

        (and times?# check-params?#)
        (let [matching-invocations# (filter #(speclj.stub/params-match? with# %) invocations#)
              matching-count#       (count matching-invocations#)]
          (when-not (= times# matching-count#)
            (-fail (str "Expected: " times# " " (invocations-str# times#) " of " name# " with " (pr-str with#) speclj.platform/endl "     got: " matching-count# " " (invocations-str# matching-count#)))))

        check-params?#
        (when-not (some #(speclj.stub/params-match? with# %) invocations#)
          (-fail (str "Expected: invocation of " name# " with " (pr-str with#) speclj.platform/endl "     got: " (pr-str invocations#))))

        times?#
        (when-not (= times# (count invocations#))
          (-fail (str "Expected: " times# " " (invocations-str# times#) " of " name# speclj.platform/endl "     got: " (count invocations#))))

        :else
        (when-not (seq invocations#)
          (-fail (str "Expected: an invocation of " name# speclj.platform/endl "     got: " (count invocations#))))

        ))))

(defmacro should-not-have-invoked
  "Inverse of should-have-invoked.

  Options:
    :times - the number of times the stub should not have been invoked. nil means never. (default: nil)
    :with - a list of arguments that the stubs should not have been invoked with.
      If not specified, anything goes. Special expected arguments include:
       :* - matches anything
       a fn - matches when the actual is the same fn or calling fn with the actual argument returns true

  Example:
  (let [foo (stub :foo)]
    (should-not-have-invoked :foo {:with [1] :times 3}) ; pass
    (foo 1)
    (should-not-have-invoked :foo {:with [1] :times 3}) ; pass
    (should-not-have-invoked :foo {:with [1] :times 1}) ; fail
    (should-not-have-invoked :foo {:times 3}) ; pass
    (should-not-have-invoked :foo {:times 1}) ; fail
    (should-not-have-invoked :foo {:with [1]}) ; fail
    )"
  ([name] `(should-not-have-invoked ~name {}))
  ([name options]
   `(let [name#          ~name
          options#       ~options
          invocations#   (speclj.stub/invocations-of name#)
          times#         (:times options#)
          times?#        (number? times#)
          check-params?# (contains? options# :with)
          with#          (:with options#)
          with#          (if (nil? with#) [] with#)
          add-s#         #(if (= 1 %) "" "s")]
      (cond
        (and times?# check-params?#)
        (let [matching-invocations# (filter #(speclj.stub/params-match? with# %) invocations#)
              matching-count#       (count matching-invocations#)]
          (when (= times# matching-count#)
            (-fail (str "Expected: " name# " not to have been invoked " times# " time" (add-s# matching-count#) " with " (pr-str with#) speclj.platform/endl "     got: " matching-count# " invocation" (add-s# matching-count#)))))

        times?#
        (when (= times# (count invocations#))
          (-fail (str "Expected: " name# " not to have been invoked " times# " time" (add-s# times#) speclj.platform/endl "     got: " times# " invocation" (add-s# times#))))

        check-params?#
        (when (some #(speclj.stub/params-match? with# %) invocations#)
          (-fail (str "Expected: " name# " not to have been invoked with " (pr-str with#) speclj.platform/endl "     got: " (pr-str invocations#))))

        :else
        (when (seq invocations#)
          (-fail (str "Expected: 0 invocations of " name# speclj.platform/endl "     got: " (count invocations#))))

        ))))

(def ^:dynamic ^:no-doc *bound-by-should-invoke* false)

(defmacro ^:no-doc bound-by-should-invoke? []
  `(if-cljs
     *bound-by-should-invoke*
     (and (bound? #'*bound-by-should-invoke*)
          *bound-by-should-invoke*)))

(defmacro ^:no-doc with-stubbed-invocations [& body]
  `(if (not (speclj.platform/bound-by-should-invoke?))
     (with-redefs [speclj.stub/*stubbed-invocations*        (atom [])
                   speclj.platform/*bound-by-should-invoke* true]
       ~@body)
     (do ~@body)))

(defmacro should-invoke
  "Creates a stub, and binds it to the specified var, evaluates the body, and checks the invocations.

  (should-invoke reverse {:with [1 2 3] :return []} (reverse [1 2 3]))

  See stub and should-have-invoked for valid options."
  [var options & body]
  (when-not (map? options)
    `(throw (-new-exception "The second argument to should-invoke must be a map of options")))
  (let [var-name (str var)]
    `(let [options# ~options]
       (with-stubbed-invocations
         (with-redefs [~var (speclj.stub/stub ~var-name options#)]
           ~@body)
         (should-have-invoked ~var-name options#)))))

(defmacro should-not-invoke
  "Creates a stub, and binds it to the specified var, evaluates the body, and checks that it was NOT invoked.

  (should-not-invoke reverse {:with [1 2 3] :return [] :times 2} (reverse [1 2 3])) ; pass
  (should-not-invoke reverse {:with [1 2 3] :return []} (reverse [1 2 3])) ; fail

  See stub and should-not-have-invoked for valid options."
  [var options & body]
  (when-not (map? options)
    `(throw (-new-exception "The second argument to should-not-invoke must be a map of options")))
  (let [var-name (str var)]
    `(let [options# ~options]
       (with-stubbed-invocations
         (with-redefs [~var (speclj.stub/stub ~var-name options#)]
           ~@body)
         (should-not-have-invoked ~var-name options#)))))

(defmacro should<
  "Asserts that the first numeric form is less than the second numeric form, using the built-in < function."
  [a b]
  `(let [a# ~a b# ~b]
     (if (and (number? a#) (number? b#))
       (when-not (< a# b#) (-fail (str "expected " a# " to be less than " b# " but got: (< " a# " " b# ")")))
       (throw (-new-exception (wrong-types "should<" a# b#))))))

(defmacro should>
  "Asserts that the first numeric form is greater than the second numeric form, using the built-in > function."
  [a b]
  `(let [a# ~a b# ~b]
     (if (and (number? a#) (number? b#))
       (when-not (> a# b#) (-fail (str "expected " a# " to be greater than " b# " but got: (> " a# " " b# ")")))
       (throw (-new-exception (wrong-types "should>" a# b#))))))

(defmacro should<=
  "Asserts that the first numeric form is less than or equal to the second numeric form, using the built-in <= function."
  [a b]
  `(let [a# ~a b# ~b]
     (if (and (number? a#) (number? b#))
       (when-not (<= a# b#) (-fail (str "expected " a# " to be less than or equal to " b# " but got: (<= " a# " " b# ")")))
       (throw (-new-exception (wrong-types "should<=" a# b#))))))

(defmacro should>=
  "Asserts that the first numeric form is greater than or equal to the second numeric form, using the built-in >= function."
  [a b]
  `(let [a# ~a b# ~b]
     (if (and (number? a#) (number? b#))
       (when-not (>= a# b#) (-fail (str "expected " a# " to be greater than or equal to " b# " but got: (>= " a# " " b# ")")))
       (throw (-new-exception (wrong-types "should>=" a# b#))))))

(defmacro run-specs []
  "If evaluated outside the context of a spec run, it will run all the specs that have been evaluated using the default
runner and reporter.  A call to this function is typically placed at the end of a spec file so that all the specs
are evaluated by evaluation the file as a script.  Optional configuration parameters may be passed in:

(run-specs :stacktrace true :color false :reporter \"documentation\")"
  `(if-cljs
     (comment "Ignoring run-specs for clojurescript")
     (do
       (require '[speclj.cli]) ; require all speclj files
       (speclj.run.standard/run-specs))))

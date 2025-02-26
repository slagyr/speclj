(ns speclj.spec-helper
  #?(:cljs (:require-macros [speclj.spec-helper :refer [test-exported-meta test-get-descriptions]]))
  (:require [speclj.core #?(:cljs :refer-macros :default :refer) [context -fail it should= with]]
            [speclj.platform #?(:cljs :refer-macros :default :refer) [try-catch-anything]]
            [speclj.components :as components]
            [speclj.running :as running]))

(defmacro run-result [& body]
  `(try-catch-anything
     ~@body :pass
     (catch e# e#)))

(defmacro should-pass! [& body]
  `(let [result# (run-result ~@body)]
     (when-not (= :pass result#)
       (-fail (str "Unexpected failure: " (speclj.platform/error-message result#))))))

(defmacro should-have-assertions [n]
  `(let [assertions# @components/*assertions*]
     (should= ~n assertions#)))

(defn to-string [v] (#?(:cljr .ToString :default .toString) v))

(defmacro should-fail! [& body]
  `(let [result# (run-result ~@body)]
     (cond
       (= :pass result#) (-fail (str "Unexpected pass: " '~body))
       (not (speclj.error/failure? result#)) (-fail (str "Unexpected error: " (to-string result#))))))

(defmacro should-error! [& body]
  `(let [result# (run-result ~@body)]
     (cond
       (= :pass result#) (-fail (str "Unexpected pass: " '~body))
       (speclj.error/failure? result#) (-fail (str "Unexpected failure: " (speclj.platform/error-message result#))))))

(defmacro test-exported-meta [sym]
  `(it '~sym
     (let [var# #'~sym]
       (when-not (true? (:export (meta var#)))
         (-fail (str "expected " var# " to ^:export"))))))

(defmacro failure-message [& body]
  `(let [result# (run-result ~@body)]
     (if (speclj.error/failure? result#)
       (speclj.platform/error-message result#)
       (-fail (str "Expected a failure but got: " result#)))))

(defn test-description-filtering [new-runner-fn]
  (context "filtering descriptions"

    (with runner (new-runner-fn))

    (it "no descriptions by an empty map"
      (running/filter-descriptions @runner {})
      (should= [] @(.-descriptions @runner)))

    (it "filters one description by an empty map"
      (running/submit-description @runner (components/new-description "Gone" false "some.ns"))
      (running/filter-descriptions @runner {})
      (should= [] @(.-descriptions @runner)))

    (it "one description by its own namespace"
      (let [one (components/new-description "One" false "some.ns")]
        (running/submit-description @runner one)
        (running/filter-descriptions @runner {"some.ns" true})
        (should= [one] @(.-descriptions @runner))))

    (it "two descriptions with one matching namespace"
      (let [one (components/new-description "One" false "some.ns")
            two (components/new-description "Two" false "some.other.ns")]
        (running/submit-description @runner one)
        (running/submit-description @runner two)
        (running/filter-descriptions @runner {"some.other.ns" true})
        (should= [two] @(.-descriptions @runner))))

    (it "three descriptions with two matching namespaces"
      (let [one   (components/new-description "One" false "some.ns")
            two   (components/new-description "Two" false "some.other.ns")
            three (components/new-description "Three" false "three.ns")]
        (running/submit-description @runner one)
        (running/submit-description @runner two)
        (running/submit-description @runner three)
        (running/filter-descriptions @runner {"three.ns" true "some.ns" true})
        (should= [one three] @(.-descriptions @runner))))

    (it "namespace is assigned a falsy value"
      (let [one   (components/new-description "One" false "some.ns")
            two   (components/new-description "Two" false "some.other.ns")
            three (components/new-description "Three" false "three.ns")]
        (running/submit-description @runner one)
        (running/submit-description @runner two)
        (running/submit-description @runner three)
        (running/filter-descriptions @runner {"three.ns" true "some.ns" false})
        (should= [three] @(.-descriptions @runner))))

    (it "does nothing when filtering by nil"
      (let [one   (components/new-description "One" false "some.ns")
            two   (components/new-description "Two" false "some.other.ns")
            three (components/new-description "Three" false "three.ns")]
        (running/submit-description @runner one)
        (running/submit-description @runner two)
        (running/submit-description @runner three)
        (running/filter-descriptions @runner nil)
        (should= [one two three] @(.-descriptions @runner))))

    #?(:cljs
       (it "filters by JS object"
         (let [one   (components/new-description "One" false "some.ns")
               two   (components/new-description "Two" false "some.other.ns")
               three (components/new-description "Three" false "three.ns")]
           (running/submit-description @runner one)
           (running/submit-description @runner two)
           (running/submit-description @runner three)
           (running/filter-descriptions @runner (js-obj "three.ns" true "some.ns" true))
           (should= [one three] @(.-descriptions @runner))))
       )
    )
  )

(defmacro test-get-descriptions [new-runner-fn]
  `(it (str '~new-runner-fn " fetches descriptions")
     (let [runner# (~new-runner-fn)
           one#    (components/new-description "One" false "some.ns")
           two#    (components/new-description "Two" false "some.other.ns")
           three#  (components/new-description "Three" false "three.ns")]
       (should= [] (vec (running/get-descriptions runner#)))
       (running/submit-description runner# one#)
       (should= [one#] (vec (running/get-descriptions runner#)))
       (running/submit-description runner# three#)
       (should= [one# three#] (vec (running/get-descriptions runner#)))
       (running/submit-description runner# two#)
       (should= [one# three# two#] (vec (running/get-descriptions runner#))))))

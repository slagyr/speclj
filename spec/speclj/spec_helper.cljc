(ns speclj.spec-helper
  #?(:cljs (:require-macros [speclj.spec-helper :refer [test-description-filtering]]))
  (:require [speclj.core #?(:clj :refer :cljs :refer-macros) [context -fail it should= with]]
            [speclj.platform #?(:clj :refer :cljs :refer-macros) [try-catch-anything]]
            [speclj.components :as components]
            [speclj.running :as running]))

(defmacro run-result [& body]
  `(try-catch-anything
     ~@body :pass
     (catch e# e#)))

(defmacro should-pass! [& body]
  `(let [result# (run-result ~@body)]
     (if (not (= :pass result#))
       (-fail (str "Unexpected failure: " (speclj.platform/error-message result#))))))

(defmacro should-fail! [& body]
  `(let [result# (run-result ~@body)]
     (cond
       (= :pass result#) (-fail (str "Unexpected pass: " '~body))
       (not (speclj.platform/failure? result#)) (-fail (str "Unexpected error: " (.toString result#))))))

(defmacro should-error! [& body]
  `(let [result# (run-result ~@body)]
     (cond
       (= :pass result#) (-fail (str "Unexpected pass: " '~body))
       (speclj.platform/failure? result#) (-fail (str "Unexpected failure: " (speclj.platform/error-message result#))))))

(defmacro test-exported-meta [sym]
  `(it '~sym
     (let [var# #'~sym]
       (when (not= true (:export (meta var#)))
         (-fail (str "expected " var# " to ^:export"))))))

(defmacro failure-message [& body]
  `(let [result# (run-result ~@body)]
     (if (not (speclj.platform/failure? result#))
       (-fail (str "Expected a failure but got: " result#))
       (speclj.platform/error-message result#))))

(defmacro test-description-filtering [new-runner-fn]
  `(context "filtering descriptions"

     (with runner# (~new-runner-fn))

     (it "no descriptions by an empty list"
       (running/filter-descriptions @runner# [])
       (should= [] @(.-descriptions @runner#)))

     (it "filters one description by an empty list"
       (running/submit-description @runner# (components/new-description "Gone" false "some.ns"))
       (running/filter-descriptions @runner# [])
       (should= [] @(.-descriptions @runner#)))

     (it "one description by its own namespace"
       (let [one# (components/new-description "One" false "some.ns")]
         (running/submit-description @runner# one#)
         (running/filter-descriptions @runner# ["some.ns"])
         (should= [one#] @(.-descriptions @runner#))))

     (it "two descriptions with one matching namespace"
       (let [one# (components/new-description "One" false "some.ns")
             two# (components/new-description "Two" false "some.other.ns")]
         (running/submit-description @runner# one#)
         (running/submit-description @runner# two#)
         (running/filter-descriptions @runner# ["some.other.ns"])
         (should= [two#] @(.-descriptions @runner#))))

     (it "three descriptions with two matching namespaces"
       (let [one#   (components/new-description "One" false "some.ns")
             two#   (components/new-description "Two" false "some.other.ns")
             three# (components/new-description "Three" false "three.ns")]
         (running/submit-description @runner# one#)
         (running/submit-description @runner# two#)
         (running/submit-description @runner# three#)
         (running/filter-descriptions @runner# ["three.ns" "some.ns"])
         (should= [one# three#] @(.-descriptions @runner#))))

     (it "does nothing when filtering by nil"
       (let [one#   (components/new-description "One" false "some.ns")
             two#   (components/new-description "Two" false "some.other.ns")
             three# (components/new-description "Three" false "three.ns")]
         (running/submit-description @runner# one#)
         (running/submit-description @runner# two#)
         (running/submit-description @runner# three#)
         (running/filter-descriptions @runner# nil)
         (should= [one# two# three#] @(.-descriptions @runner#))))
     ))

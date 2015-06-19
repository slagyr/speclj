(ns speclj.spec-helper
  (:require [speclj.core :refer [-fail]]))

;cljs? is a duplicate of the cljs? found in core
;this decision was made so that the Core namespace API and dependencies are simple as possible
(def cljs? (boolean (find-ns 'cljs.analyzer)))

(defmacro run-result [& body]
  `(try
     ~@body
     :pass
     ~(if cljs?
        '(catch :default e# e#)
        '(catch java.lang.Throwable e# e#))))

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

(defmacro failure-message [& body]
  `(let [result# (run-result ~@body)]
     (if (not (speclj.platform/failure? result#))
       (-fail (str "Expected a failure but got: " result#))
       (speclj.platform/error-message result#))))

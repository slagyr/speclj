(ns speclj.spec-helper
  (#?(:clj :require :cljs :require-macros)
   [speclj.core :refer [-fail it]]
   [speclj.platform :refer [try-catch-anything]]))

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

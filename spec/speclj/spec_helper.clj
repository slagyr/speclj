(ns speclj.spec-helper
  (:require ; comment here to prevent cljs translation to replace with require-macros
    [speclj.core :refer [-fail]]))

(defmacro run-result [& body]
  `(try
     ~@body
     :pass (catch java.lang.Throwable e#
             e#)))

(defmacro should-pass! [& body]
  `(let [result# (run-result ~@body)]
     (if (not (= :pass result#))
       (-fail (str "Unexpected failure: " (speclj.platform/error-message result#))))))

(defmacro should-fail! [& body]
  `(let [result# (run-result ~@body)]
     (cond
       (= :pass result#) (-fail (str "Unexpected pass: " '~body))
       (not (= speclj.platform/failure (type result#))) (-fail (str "Unexpected error: " (.toString result#))))))

(defmacro should-error! [& body]
  `(let [result# (run-result ~@body)]
     (cond
       (= :pass result#) (-fail (str "Unexpected pass: " '~body))
       (= speclj.platform/failure (type result#)) (-fail (str "Unexpected failure: " (speclj.platform/error-message result#))))))

(defmacro failure-message [& body]
  `(let [result# (run-result ~@body)]
     (if (not (= speclj.platform/failure (type result#)))
       (-fail (str "Expected a failure but got: " result#))
       (speclj.platform/error-message result#))))
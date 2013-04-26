(ns speclj.spec-helper
  (:require [speclj.core :refer [-fail]])
  (:import [java.io File]))

(defmacro run-result [& body]
  `(try
     ~@body
     :pass (catch Throwable e#
             e#)))

(defmacro should-pass! [& body]
  `(let [result# (run-result ~@body)]
     (if (not (= :pass result#))
       (-fail (str "Unexpected failure: " (.getMessage result#))))))

(defmacro should-fail! [& body]
  `(let [result# (run-result ~@body)]
     (cond
       (= :pass result#) (-fail (str "Unexpected pass: " '~body))
       (not (= AssertionError (class result#))) (-fail (str "Unexpected error: " (.toString result#))))))

(defmacro should-error! [& body]
  `(let [result# (run-result ~@body)]
     (cond
       (= :pass result#) (-fail (str "Unexpected pass: " '~body))
       (= AssertionError (class result#)) (-fail (str "Unexpected failure: " (.getMessage result#))))))

(defmacro failure-message [& body]
  `(let [result# (run-result ~@body)]
     (if (not (= AssertionError (class result#)))
       (-fail (str "Expected a failure but got: " result#))
       (.getMessage result#))))

(defn find-dir
  ([name] (find-dir (File. (.getCanonicalPath (File. ""))) name))
  ([file name]
    (let [examples (File. file name)]
      (if (.exists examples)
        examples
        (find-dir (.getParentFile file) name)))))
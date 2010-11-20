(ns speclj.spec-helper
  (:import
    [speclj SpecFailure]
    [java.io File]))

(defmacro run-result [& body]
  `(try
    ~@body
    :pass
    (catch Exception e#
      e#)))

(defmacro should-pass! [& body]
  `(let [result# (run-result ~@body)]
    (if (not (= :pass result#))
      (throw (SpecFailure. (str "Unexpected failure: " (.getMessage result#)))))))

(defmacro should-fail! [& body]
  `(let [result# (run-result ~@body)]
    (cond
      (= :pass result#) (throw (SpecFailure. (str "Unexpected pass: " '~body)))
      (not (= SpecFailure (class result#))) (throw (SpecFailure. (str "Unexpected error: " (.toString result#)))))))

(defmacro should-error! [& body]
  `(let [result# (run-result ~@body)]
    (cond
      (= :pass result#) (throw (SpecFailure. (str "Unexpected pass: " '~body)))
      (= SpecFailure (class result#)) (throw (SpecFailure. (str "Unexpected failure: " (.getMessage result#)))))))

(defmacro failure-message [& body]
  `(let [result# (run-result ~@body)]
    (if (not (= SpecFailure (class result#)))
      (throw (SpecFailure. (str "Expected a failure but got: " result#)))
      (.getMessage result#))))

(defn find-dir
  ([name] (find-dir (File. (.getCanonicalPath (File. ""))) name))
  ([file name]
    (let [examples (File. file name)]
      (if (.exists examples)
        examples
        (find-dir (.getParentFile file) name)))))
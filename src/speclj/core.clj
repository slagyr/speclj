(ns speclj.core
  (:use
    [speclj.running :only (submit-description default-runner active-runner report)]
    [speclj.reporting :only (active-reporter)]
    [speclj.components]
    [speclj.util :only (endl)])
  (:require
    [speclj.run.standard]
    [speclj.report.progress])
  (:import [speclj SpecFailure]))

(defmacro it [name & body]
  `(new-characteristic ~name (fn [] ~@body)))

(defn describe [name & parts]
  (let [description (new-description name)]
    (doseq [part parts] (install part description))
    (submit-description description)))

(defmacro before [& body]
  `(new-before (fn [] ~@body)))

(defmacro after [& body]
  `(new-after (fn [] ~@body)))

(defmacro around [bindings & body]
  `(new-around (fn ~bindings ~@body)))

(defmacro before-all [& body]
  `(new-before-all (fn [] ~@body)))

(defmacro after-all [& body]
  `(new-after-all (fn [] ~@body)))

(defmacro with [name & body]
  `(do
;    (if ~(resolve name)
;      (println (str "WARNING: the symbol #'" '~(name name) " is already declared"))) ;TODO MDM Need to report this warning
    (let [with-component# (new-with '~name (fn [] ~@body))]
      (def ~(symbol name) with-component#)
      with-component#)))

(defmacro should [expr]
  `(let [value# ~expr]
    (if-not value#
      (throw (SpecFailure. (str "Expected truthy but was: <" value# ">"))))))


(defmacro should-not [expr]
  `(let [value# ~expr]
    (if value#
      (throw (SpecFailure. (str "Expected falsy but was: <" value# ">"))))))

(defmacro should= [expr1 expr2]
  `(let [expected# ~expr1 actual# ~expr2]
    (if (not (= expected# actual#))
      (throw (SpecFailure. (str "Expected: <" expected# ">" endl "     got: <" actual# "> (using =)"))))))

(defmacro should-not= [expr1 expr2]
  `(let [expected# ~expr1 actual# ~expr2]
    (if (= expected# actual#)
      (throw (SpecFailure. (str "Expected: <" expected# ">" endl "not to =: <" actual# ">"))))))

(defmacro should-fail
  ([] `(should-fail "Forced failure"))
  ([message] `(throw (SpecFailure. ~message))))

(defmacro -create-should-throw-failure [expected actual expr]
  `(let [expected-name# (.getName ~expected)
         expected-gaps# (apply str (repeat (count expected-name#) " "))
         actual-string# (if ~actual (.toString ~actual) "<nothing thrown>")]
    (SpecFailure. (str "Expected " expected-name# " thrown from: " ~expr endl
      "         " expected-gaps# "     but got: " actual-string#))))

(defmacro should-throw
  ([expr] `(should-throw Throwable ~expr))
  ([throwable-type expr]
    `(try
      ~expr
      (throw (-create-should-throw-failure ~throwable-type nil '~expr))
      (catch Throwable e#
        (cond
          (.isInstance SpecFailure e#) (throw e#)
          (not (.isInstance ~throwable-type e#)) (throw (-create-should-throw-failure ~throwable-type e# '~expr))
          :else e#))))
  ([throwable-type message expr]
    `(let [e# (should-throw ~throwable-type ~expr)]
      (try
        (should= ~message (.getMessage e#))
        (catch SpecFailure f# (throw (SpecFailure. (str "Expected exception message didn't match" endl (.getMessage f#)))))))))

(defmacro should-not-throw [expr]
  `(try
    ~expr
    (catch Throwable e# (throw (SpecFailure. (str "Expected nothing thrown from: " '~expr endl
      "                     but got: " (.toString e#)))))))

(defn conclude-single-file-run []
  (if (identical? (active-runner) @default-runner)
    (report (active-runner) (active-reporter))))
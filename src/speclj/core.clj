(ns speclj.core
  (:use
    [speclj.running :only (submit-description)]
    [speclj.components]
    [speclj.util :only (endl)])
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
    (if ~(resolve name)
      (println (str "WARNING: the symbol #'" ~(name name) " is already declared"))) ;TODO MDM Need to report this warning
    (let [with-component# (new-with '~name (fn [] ~@body))]
      (def ~(symbol name) with-component#)
      with-component#)))

(defmacro should [expr]
  `(let [value# ~expr]
    (if-not value#
      (throw (SpecFailure. (str "Expected truthy but was: <" value# ">"))))))

(defmacro should= [expr1 expr2]
  `(let [expected# ~expr1 actual# ~expr2]
    (if (not (= expected# actual#))
      (throw (SpecFailure. (str "Expected: <" expected# ">" endl "     got: <" actual# "> (using =)"))))))

(defn conclude-single-file-run []
  (if (identical? (speclj.running/active-runner) speclj.running/default-runner)
    (speclj.running/report (speclj.running/active-runner) (speclj.reporting/active-reporter))))
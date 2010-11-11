(ns speclj.core
  (:use
    [speclj.running :only (submit-description)]
    [speclj.components])
  (:import [speclj SpecFailure]))

(declare *description*)

(defmacro it [name & body]
  `(new-characteristic ~name (fn [] ~@body)))

(defn describe [name & parts]
  (let [description (new-description name)]
    (binding [*description* description]
      (doseq [part parts] (install part description))
      (submit-description description))))

(defmacro before [& body]
  `(new-before (fn [] ~@body)))

(defmacro after [& body]
  `(new-after (fn [] ~@body)))

(defmacro before-all [& body]
  `(new-before-all (fn [] ~@body)))

(defmacro after-all [& body]
  `(new-after-all (fn [] ~@body)))

(defmacro with [name & body]
  `(do
    (if ~(resolve name)
      (println (str "WARNING: the symbol #'" ~(name name) " is already declared")))  ;TODO MDM Need to report this warning
    (let [with-component# (new-with '~name (fn [] ~@body))]
      (def ~(symbol name) with-component#)
      with-component#)))

(defmacro should [expr]
  `(if-not ~expr
    (throw (SpecFailure. (str "Expected " '~expr " to be truthy")))))

(defn conclude-single-file-run []
  (if (identical? (speclj.running/active-runner) speclj.running/default-runner)
    (speclj.running/report (speclj.running/active-runner) (speclj.reporting/active-reporter))))
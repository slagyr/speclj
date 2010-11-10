(ns mmspec.core
  (:use
    [mmspec.running :only (submit-description)]
    [mmspec.components]))

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
    (let [~'with-component (new-with '~name (fn [] ~@body))]
      (def ~(symbol name) ~'with-component)
      ~'with-component)))

(defmacro should [expr]
  ;  (println expr)
  `(if-not ~expr
    (throw (Exception. (str "Expected " '~expr " to be truthy")))))
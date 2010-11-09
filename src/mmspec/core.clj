(ns mmspec.core
  (:use
    [mmspec.running :only (submit-description)]
    [mmspec.components :only (install)])
  (:import
    [mmspec.components Description Characteristic]))

(declare *description*)

(defmacro it [name & body]
  `(Characteristic. ~name (atom nil) (fn [] ~@body)))

(defn describe [name & parts]
  (let [description (Description. name (atom []) (atom []))]
    (binding [*description* description]
      (doseq [part parts] (install part description))
      (submit-description description))))

(defmacro should [expr]
  (println expr)
  `(if-not ~expr
    (throw (Exception. (str "Expected " '~expr " to be truthy")))))
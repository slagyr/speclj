(ns speclj.platform
  (:require [clojure.string :as str]))

(def endl "\n")
(def file-separator "/")

(def re-type (type #"."))

(defn re? [obj] (= re-type (type obj)))

(deftype SpecFailure [message])
;(set! specljs.platform.SpecFailure/prototype (js/Error.))
;(set! (.-constructor specljs.platform.SpecFailure/prototype) SpecFailure)
(deftype SpecPending [message])

(declare ^:dynamic *bound-by-should-invoke*)

(defn bound-by-should-invoke? []
  *bound-by-should-invoke*)

(def throwable js/Object)
(def exception js/Error)
(def failure SpecFailure)
(def pending SpecPending)

(defn difference-greater-than-delta? [expected actual delta]
  (> (js/Math.abs (- expected actual)) (js/Math.abs delta)))

(defn pending? [e] (isa? (type e) pending))
(defn failure? [e] (isa? (type e) failure))

(defn error-message [e] (.-message e))
(defn failure-source [e]
  (cond
    (.-fileName e) (str (.-fileName e) ":" (or (.-lineNumber e) "?"))
    (.-stack e) (str/trim (nth (str/split-lines (.-stack e)) (count (str/split-lines (.-message e)))))
    :else "unkown-file:?"))
(defn stack-trace [e] (rest (str/split-lines (or (.-stack e) (.toString e)))))
(defn cause [e] nil)
(defn print-stack-trace [e] (println (or (.-stack e) "missing stack trace")))
(defn elide-level? [stack-element] false)

(defn type-name [t] (if t (.-name t) "nil"))

(defn format-seconds [secs] (.toFixed secs 5))
(defn current-time [] (.getTime (js/Date.)))
(defn secs-since [start] (/ (- (.getTime (js/Date.)) start) 1000.0))

(set! *print-fn* (fn [thing] (.log js/console thing)))

(defn dynamically-invoke [ns-name fn-name]
  (let [code (str (str/replace ns-name "-" "_") "." (str/replace fn-name "-" "_") "();")]
    (js* "eval(~{code})")))


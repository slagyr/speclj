(ns speclj.platform
  (:require [clojure.string :as str]))

(def endl "\n")
(def file-separator "/")
(def source-file-regex #".*\.clj(c|s)")

(defn re? [obj] (instance? js/RegExp obj))

(declare ^:dynamic *bound-by-should-invoke*)

(defn bound-by-should-invoke? []
  *bound-by-should-invoke*)

(def throwable js/Object)
(def exception js/Error)

(defn difference-greater-than-delta? [expected actual delta]
  (> (abs (- expected actual)) (abs delta)))

(defn failure-source-str [e]
  (cond
    (.-fileName e) (str (.-fileName e) ":" (or (.-lineNumber e) "?"))
    (.-stack e) (str/trim (nth (str/split-lines (.-stack e)) (count (str/split-lines (.-message e)))))
    :else "unkown-file:?"))

(defn error-message [e] (.-message e))
(defn error-str [e] (str e))
(defn stack-trace [e] (rest (str/split-lines (or (.-stack e) (.toString e)))))
(defn cause [e] (.-cause e))
(defn print-stack-trace [e] (println (or (.-stack e) "missing stack trace")))
(defn elide-level? [_stack-element] false)

(defn type-name [t] (if t (.-name t) "nil"))

(defn format-seconds [secs] (.toFixed secs 5))
(defn current-time [] (.getTime (js/Date.)))
(defn secs-since [start] (/ (- (.getTime (js/Date.)) start) 1000.0))

(set! *print-fn* (fn [thing] (.log js/console thing)))

(defn dynamically-invoke [ns-name fn-name]
  (let [code (str (str/replace ns-name "-" "_") "." (str/replace fn-name "-" "_") "();")]
    (js* "eval(~{code})")))

(defn get-name [ns] (.name ns))

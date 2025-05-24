(ns speclj.reporting
  (:require [clojure.string :as str]
            #?(:cljs [goog.string]) ;cljs bug?
            [speclj.config :refer [*color?* *full-stack-trace?*]]
            [speclj.platform :refer [endl stack-trace cause error-str print-stack-trace elide-level?]]
            [speclj.results :refer [pass? fail?]]))

(defn- sum-by [f coll] (apply + (map f coll)))
(defn tally-time [results] (sum-by #(.-seconds %) results))
(defn tally-assertions [results] (sum-by #(.-assertions %) results))

(defprotocol Reporter
  (report-message [reporter message])
  (report-description [this description])
  (report-pass [this result])
  (report-pending [this result])
  (report-fail [this result])
  (report-runs [this results])
  (report-error [this exception]))

(defmulti report-run (fn [result _reporters] (type result)))
(defmethod report-run speclj.results.PassResult [result reporters]
  (doseq [reporter reporters]
    (report-pass reporter result)))
(defmethod report-run speclj.results.FailResult [result reporters]
  (doseq [reporter reporters]
    (report-fail reporter result)))
(defmethod report-run speclj.results.PendingResult [result reporters]
  (doseq [reporter reporters]
    (report-pending reporter result)))
(defmethod report-run speclj.results.ErrorResult [result reporters]
  (doseq [reporter reporters]
    (report-error reporter result)))

(defn- stylizer [code text]
  (if *color?*
    (str "\u001b[" code "m" text "\u001b[0m")
    text))

(defn red [text] (stylizer "31" text))
(defn green [text] (stylizer "32" text))
(defn yellow [text] (stylizer "33" text))
(defn grey [text] (stylizer "90" text))

(defn- print-elides [n]
  (when (pos? n)
    (println "\t..." n "stack levels elided ...")))

(declare print-exception)

(defn- print-stack-levels [exception]
  (loop [levels (stack-trace exception) elides 0]
    (if (seq levels)
      (let [level (first levels)]
        (if (elide-level? level)
          (recur (rest levels) (inc elides))
          (do
            (print-elides elides)
            (println "\tat" (str level))
            (recur (rest levels) 0))))
      (print-elides elides)))
  (when-let [cause (cause exception)]
    (print-exception "Caused by:" cause)))

(defn- print-exception [prefix exception]
  (if prefix
    (println prefix (error-str exception))
    (println (error-str exception)))
  (print-stack-levels exception))

(defn stack-trace-str [exception]
  (with-out-str
    (if *full-stack-trace?*
      (print-stack-trace exception)
      (print-exception nil exception))))

(defn prefix [pre & args]
  (let [value          (apply str args)
        lines          (str/split value #"[\r\n]+")
        prefixed-lines (map #(str pre %) lines)]
    (str/join endl prefixed-lines)))

(defn indent [n & args]
  (let [spaces    (int (* n 2.0))
        indention (reduce (fn [p _] (str " " p)) "" (range spaces))]
    (apply prefix indention args)))

(defn report-description* [reporters description]
  (doseq [reporter reporters]
    (report-description reporter description)))

(defn report-runs* [reporters results]
  (doseq [reporter reporters]
    (report-runs reporter results)))

(defn report-message* [reporters message]
  (doseq [reporter reporters]
    (report-message reporter message)))

(defn report-error* [reporters exception]
  (doseq [reporter reporters]
    (report-error reporter exception)))

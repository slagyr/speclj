(ns speclj.reporting
  (:require [clojure.string :as string :refer [split join]]
            #?(:cljs [goog.string])                         ;cljs bug?
            [speclj.config :refer [*reporters* *color?* *full-stack-trace?*]]
            [speclj.platform :refer [endl file-separator failure-source stack-trace cause print-stack-trace elide-level?]]
            [speclj.results :refer [pass? fail?]]))

(defn tally-time [results]
  (apply + (map #(.-seconds %) results)))

(defprotocol Reporter
  (report-message [reporter message])
  (report-description [this description])
  (report-pass [this result])
  (report-pending [this result])
  (report-fail [this result])
  (report-runs [this results])
  (report-error [this exception]))

(defmulti report-run (fn [result reporters] (type result)))
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

(defn- stylizer [code]
  (fn [text]
    (if *color?*
      (str "\u001b[" code "m" text "\u001b[0m")
      text)))

(def red (stylizer "31"))
(def green (stylizer "32"))
(def yellow (stylizer "33"))
(def grey (stylizer "90"))

(defn- print-elides [n]
  (if (pos? n)
    (println "\t..." n "stack levels elided ...")))

(declare print-exception)

#?(:clj
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
     (if-let [cause (cause exception)]
       (print-exception "Caused by:" cause)))

   :cljs
   (defn- print-stack-levels [exception]
     (loop [levels (stack-trace exception) elides 0]
       (if (seq levels)
         (let [level (first levels)]
           (if (elide-level? level)
             (recur (rest levels) (inc elides))
             (do
               (print-elides elides)
               (println (str level))
               (recur (rest levels) 0))))
         (print-elides elides)))
     (if-let [cause (cause exception)]
       (print-exception "Caused by:" cause))))

(defn- print-exception [prefix exception]
  (if prefix
    (println prefix (str exception))
    (println (str exception)))
  (print-stack-levels exception))

(defn stack-trace-str [exception]
  (with-out-str
    (if *full-stack-trace?*
      (print-stack-trace exception)
      (print-exception nil exception))))

(defn prefix [pre & args]
  (let [value          (apply str args)
        lines          (split value #"[\r\n]+")
        prefixed-lines (map #(str pre %) lines)]
    (join endl prefixed-lines)))

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

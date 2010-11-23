(ns speclj.report.progress
  (:use
    [speclj.reporting :only (failure-source tally-time default-reporter)]
    [speclj.exec :only (pass? fail?)])
  (:import
    [speclj.reporting Reporter]
    [speclj SpecFailure]))

(defn print-failure [id result]
  (let [characteristic (.characteristic result)
        description @(.description characteristic)
        failure (.failure result)]
    (println)
    (println (str id ")"))
    (println (str "'" (.name description) " " (.name characteristic) "' FAILED"))
    (println (.getMessage failure))
    (if (.isInstance SpecFailure failure)
      (println (failure-source failure))
      (.printStackTrace failure System/out))))

(defn print-failures [results]
  (println)
  (let [failures (vec (filter fail? results))]
    (dotimes [i (count failures)]
      (print-failure (inc i) (nth failures i)))))

(def seconds-format (java.text.DecimalFormat. "0.00000"))

(defn- print-duration [results]
  (println)
  (println "Finished in" (.format seconds-format (tally-time results)) "seconds"))

(defn- print-tally [results]
  (println)
  (let [fails (reduce #(if (fail? %2) (inc %) %) 0 results)]
    (println (count results) "examples," fails "failures")))

(defn print-summary [results]
  (print-failures results)
  (print-duration results)
  (print-tally results))

(deftype ProgressReporter []
  Reporter
  (report-message [this message]
    (println message))
  (report-description [this description])
  (report-pass [this result]
    (print ".") (flush))
  (report-fail [this result]
    (print "F") (flush))
  (report-runs [this results]
    (print-summary results)))

(defn new-progress-reporter []
  (ProgressReporter.))

(swap! default-reporter (fn [_] (new-progress-reporter)))
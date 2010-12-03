(ns speclj.report.progress
  (:use
    [speclj.reporting :only (failure-source tally-time red green)]
    [speclj.exec :only (pass? fail?)]
    [speclj.util :only (seconds-format)]
    [speclj.config :only (default-reporter)])
  (:import
    [speclj.reporting Reporter]
    [speclj SpecFailure]))

(defn full-name [characteristic]
  (loop [context @(.parent characteristic) name (.name characteristic)]
    (if context
      (recur @(.parent context) (str (.name context) " " name))
      name)))

(defn print-failure [id result]
  (let [characteristic (.characteristic result)
        failure (.failure result)]
    (println)
    (println (str id ")"))
    (println (red (str "'" (full-name characteristic) "' FAILED")))
    (println (red (.getMessage failure)))
    (if (.isInstance SpecFailure failure)
      (println (failure-source failure))
      (.printStackTrace failure System/out))))

(defn print-failures [results]
  (println)
  (let [failures (vec (filter fail? results))]
    (dotimes [i (count failures)]
      (print-failure (inc i) (nth failures i)))))

(defn- print-duration [results]
  (println)
  (println "Finished in" (.format seconds-format (tally-time results)) "seconds"))

(defn- print-tally [results]
  (println)
  (let [fails (reduce #(if (fail? %2) (inc %) %) 0 results)
        color-fn (if (= 0 fails) green red)]
    (println (color-fn (str (count results) " examples, " fails " failures")))))

(defn print-summary [results]
  (print-failures results)
  (print-duration results)
  (print-tally results))

(deftype ProgressReporter []
  Reporter
  (report-message [this message]
    (println message)(flush))
  (report-description [this description])
  (report-pass [this result]
    (print (green ".")) (flush))
  (report-fail [this result]
    (print (red "F")) (flush))
  (report-runs [this results]
    (print-summary results)))

(defn new-progress-reporter []
  (ProgressReporter.))

(swap! default-reporter (fn [_] (new-progress-reporter)))
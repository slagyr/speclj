(ns speclj.report.progress
  (:use
    [speclj.reporting :only (failure-source tally-time red green yellow print-stack-trace)]
    [speclj.exec :only (pass? fail? pending?)]
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
      (print-stack-trace failure *out*)
      )))

(defn print-failures [results]
  (println)
  (let [failures (vec (filter fail? results))]
    (dotimes [i (count failures)]
      (print-failure (inc i) (nth failures i)))))

(defn- print-duration [results]
  (println)
  (println "Finished in" (.format seconds-format (tally-time results)) "seconds"))

(defn- tally [results]
  (reduce (fn [tally result]
            (cond (pending? result) (update-in tally [:pending] inc)
                  (fail? result) (update-in tally [:fail] inc)
                  :else (update-in tally [:pass] inc)))
            {:pending 0 :fail 0 :pass 0}
            results))

(defn color-fn-for [tally]
  (cond (not= 0 (:fail tally)) red
        (not= 0 (:pending tally)) yellow
        :else green))

(defn- describe-counts-for [tally]
  (let [always-on-counts [(str (apply + (vals tally)) " examples")
                          (str (:fail tally) " failures")]]
    (apply str
      (interpose ", "
        (if (> (:pending tally) 0)
            (conj always-on-counts (str (:pending tally) " pending"))
            always-on-counts)))))

(defn- print-tally [results]
  (println)
  (let [tally (tally results)
        color-fn (color-fn-for tally)]
    (println (color-fn (describe-counts-for tally)))))

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
  (report-pending [this result]
    (print (yellow "*")) (flush))
  (report-fail [this result]
    (print (red "F")) (flush))
  (report-runs [this results]
    (print-summary results)))

(defn new-progress-reporter []
  (ProgressReporter.))

(swap! default-reporter (fn [_] (new-progress-reporter)))

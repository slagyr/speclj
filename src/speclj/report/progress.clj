(ns speclj.report.progress
  (:use
    [speclj.reporting :only (failure-source tally-time red green yellow grey stack-trace indent prefix)]
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
    (println (indent 1 id ") " (full-name characteristic)))
    (println (red (indent 2.5 (.getMessage failure))))
    (if (.isInstance SpecFailure failure)
      (println (grey (indent 2.5 "; " (failure-source failure))))
      (println (grey (indent 2.5 (prefix "; " (stack-trace failure))))))))

(defn print-failures [failures]
  (when (seq failures)
    (println)
    (println "Failures:"))
  (dotimes [i (count failures)]
    (print-failure (inc i) (nth failures i))))

(defn print-pendings [pending-results]
  (when (seq pending-results)
    (println)
    (println "Pending:"))
  (doseq [result pending-results]
    (println)
    (println (yellow (str "  " (full-name (.characteristic result)))))
    (println (grey (str "    ; " (.getMessage (.exception result)))))
    (println (grey (str "    ; " (failure-source (.exception result)))))))

(defn- print-duration [results]
  (println)
  (println "Finished in" (.format seconds-format (tally-time results)) "seconds"))

(defn- categorize [results]
  (reduce (fn [tally result]
    (cond (pending? result) (update-in tally [:pending] conj result)
      (fail? result) (update-in tally [:fail] conj result)
      :else (update-in tally [:pass] conj result)))
    {:pending [] :fail [] :pass []}
    results))

(defn color-fn-for [result-map]
  (cond (not= 0 (count (:fail result-map))) red
    (not= 0 (count (:pending result-map))) yellow
    :else green))

(defn- describe-counts-for [result-map]
  (let [tally (apply hash-map (interleave (keys result-map) (map count (vals result-map))))
        always-on-counts [(str (apply + (vals tally)) " examples")
                          (str (:fail tally) " failures")]]
    (apply str
      (interpose ", "
        (if (> (:pending tally) 0)
          (conj always-on-counts (str (:pending tally) " pending"))
          always-on-counts)))))

(defn- print-tally [result-map]
  (let [color-fn (color-fn-for result-map)]
    (println (color-fn (describe-counts-for result-map)))))

(defn print-summary [results]
  (let [result-map (categorize results)]
    (print-failures (:fail result-map))
    (print-pendings (:pending result-map))
    (print-duration results)
    (print-tally result-map)))

(deftype ProgressReporter []
  Reporter
  (report-message [this message]
    (println message) (flush))
  (report-description [this description])
  (report-pass [this result]
    (print (green ".")) (flush))
  (report-pending [this result]
    (print (yellow "*")) (flush))
  (report-fail [this result]
    (print (red "F")) (flush))
  (report-runs [this results]
    (println)
    (print-summary results)))

(defn new-progress-reporter []
  (ProgressReporter.))

(swap! default-reporter (fn [_] (new-progress-reporter)))

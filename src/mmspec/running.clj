(ns mmspec.running
  (:use
    [mmspec.exec :only (pass-result fail-result)]
    [mmspec.reporting :only (report-runs report-pass report-fail active-reporter)]))

(defn- run-characteristic [characteristic reporter]
  (try
    ((.body characteristic))
    (report-pass reporter)
    (pass-result characteristic)
    (catch Exception e
      (report-fail reporter)
      (fail-result characteristic e))))

(defn- run-description
  [description reporter]
  (doall
    (for [characteristic @(.charcteristics description)]
      (run-characteristic characteristic reporter))))

(defprotocol Runner
  (run [this description reporter])
  (report [this reporter]))

(deftype SingleRunner []
  Runner
  (run [this description reporter]
    (let [results (run-description description reporter)]
      (report-runs reporter results)))
  (report [this reporter]
    ))

(deftype MultiRunner [results]
  Runner
  (run [this description reporter]
    (let [run-results (run-description description reporter)]
      (swap! results (fn [_] into _ run-results))))
  (report [this reporter]
    (report-runs reporter results)))

(declare *runner*)

(defn submit-description [description]
  (if (bound? #'*runner*)
    (run *runner* description (active-reporter))
    (run (SingleRunner.) description (active-reporter))))

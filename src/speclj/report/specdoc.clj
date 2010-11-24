(ns speclj.report.specdoc
  (:use
    [speclj.reporting :only (failure-source tally-time default-reporter)]
    [speclj.exec :only (pass? fail?)]
    [speclj.report.progress :only (print-summary)]
    [speclj.util :only (endl)])
  (:import
    [speclj.reporting Reporter]
    [speclj SpecFailure]))

(deftype SpecdocReporter []
  Reporter
  (report-message [this message]
    (println message)(flush))
  (report-description [this description]
    (println)
    (println (.name description))(flush))
  (report-pass [this result]
    (println (str "- " (.name (.characteristic result))))(flush))
  (report-fail [this result]
    (println (str "- " (.name (.characteristic result)) " (FAILED)"))(flush))
  (report-runs [this results]
    (print-summary results)))

(defn new-specdoc-reporter []
  (SpecdocReporter.))
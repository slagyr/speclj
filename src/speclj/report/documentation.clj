(ns speclj.report.documentation
  (:use
    [speclj.reporting :only (failure-source tally-time green red yellow indent)]
    [speclj.results :only (pass? fail?)]
    [speclj.report.progress :only (print-summary)]
    [speclj.util :only (endl)]
    [speclj.config :only (default-reporters)])
  (:import
    [speclj.reporting Reporter]
    [speclj SpecFailure]))

(defn level-of [component]
  (loop [component @(.parent component) level 0]
    (if component
      (recur @(.parent component) (inc level))
      level)))

(deftype DocumentationReporter []
  Reporter
  (report-message [this message]
    (println message) (flush))
  (report-description [this description]
    (let [level (level-of description)]
      (when (= 0 level) (println))
      (println (str (indent level) (.name description))) (flush)))
  (report-pass [this result]
    (let [characteristic (.characteristic result)
          level (level-of characteristic)]
      (println (green (indent (dec level) "- " (.name characteristic)))) (flush)))
  (report-pending [this result]
    (let [characteristic (.characteristic result)
          level (level-of characteristic)]
      (println (yellow (indent (dec level) "- " (.name characteristic) " (PENDING: " (.getMessage (.exception result)) ")"))) (flush)))
  (report-fail [this result]
    (let [characteristic (.characteristic result)
          level (level-of characteristic)]
      (println (red (indent (dec level) "- " (.name characteristic) " (FAILED)"))) (flush)))
  (report-runs [this results]
    (print-summary results)))

(defn new-documentation-reporter []
  (DocumentationReporter.))

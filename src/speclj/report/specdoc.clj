(ns speclj.report.specdoc
  (:use
    [speclj.reporting :only (failure-source tally-time green red yellow)]
    [speclj.exec :only (pass? fail?)]
    [speclj.report.progress :only (print-summary)]
    [speclj.util :only (endl)]
    [speclj.config :only (default-reporter)])
  (:import
    [speclj.reporting Reporter]
    [speclj SpecFailure]))

(defn level-of [component]
  (loop [component @(.parent component) level 0]
    (if component
      (recur @(.parent component) (inc level))
      level)))

(defn indent [level]
  (loop [level level indention ""]
    (if (< 0 level)
      (recur (dec level) (str "  " indention))
      indention)))

(deftype SpecdocReporter []
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
      (println (green (str (indent (dec level)) "- " (.name characteristic)))) (flush)))
  (report-pending [this result]
    (let [characteristic (.characteristic result)
          level (level-of characteristic)]
      (println (yellow (str (indent (dec level)) "- " (.name characteristic)))) (flush)))
  (report-fail [this result]
    (let [characteristic (.characteristic result)
          level (level-of characteristic)]
      (println (red (str (indent (dec level)) "- " (.name characteristic) " (FAILED)"))) (flush)))
  (report-runs [this results]
    (print-summary results)))

(defn new-specdoc-reporter []
  (SpecdocReporter.))

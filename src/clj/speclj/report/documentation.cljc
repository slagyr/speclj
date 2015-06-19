(ns speclj.report.documentation
  (:require [speclj.config :refer [default-reporters]]
            [speclj.platform :refer [endl error-message failure-source]]
            [speclj.report.progress :refer [print-summary]]
            [speclj.reporting :refer [tally-time green red yellow indent]]
            [speclj.results :refer [pass? fail?]]))

(defn level-of [component]
  (loop [component @(.-parent component) level 0]
    (if component
      (recur @(.-parent component) (inc level))
      level)))

(deftype DocumentationReporter []
  speclj.reporting/Reporter
  (report-message [this message]
    (println message) (flush))
  (report-description [this description]
    (let [level (level-of description)]
      (when (zero? level) (println))
      (println (str (indent level (.-name description)))) (flush)))
  (report-pass [this result]
    (let [characteristic (.-characteristic result)
          level (level-of characteristic)]
      (println (green (indent (dec level) "- " (.-name characteristic)))) (flush)))
  (report-pending [this result]
    (let [characteristic (.-characteristic result)
          level (level-of characteristic)]
      (println (yellow (indent (dec level) "- " (.-name characteristic) " (PENDING: " (error-message (.-exception result)) ")"))) (flush)))
  (report-fail [this result]
    (let [characteristic (.-characteristic result)
          level (level-of characteristic)]
      (println (red (indent (dec level) "- " (.-name characteristic) " (FAILED)"))) (flush)))
  (report-error [this result]
    (println (red (.toString (.-exception result)))))
  (report-runs [this results]
    (print-summary results)))

(defn new-documentation-reporter []
  (DocumentationReporter.))

(ns speclj.report.documentation
  (:require [speclj.platform :as platform]
            [speclj.report.progress :as progress]
            [speclj.reporting :refer [green indent red yellow]]))

(defn level-of [component]
  (loop [component @(.-parent component) level 0]
    (if component
      (recur @(.-parent component) (inc level))
      level)))

(defn maybe-focused [component text]
  (if-not @(.-is-focused? component) text (str text " " (yellow "[FOCUS]"))))

(deftype DocumentationReporter []
  speclj.reporting/Reporter

  (report-message [_this message]
    (println message)
    (flush))

  (report-description [_this description]
    (let [level (level-of description)]
      (when (zero? level) (println))
      (println (maybe-focused description (str (indent level (.-name description)))))
      (flush)))

  (report-pass [_this result]
    (let [characteristic (.-characteristic result)
          level          (level-of characteristic)]
      (println (maybe-focused characteristic (green (indent (dec level) "- " (.-name characteristic)))))
      (flush)))

  (report-pending [_this result]
    (let [characteristic (.-characteristic result)
          level          (level-of characteristic)]
      (println (yellow (indent (dec level) "- " (.-name characteristic) " (PENDING: " (platform/error-message (.-exception result)) ")")))
      (flush)))

  (report-fail [_this result]
    (let [characteristic (.-characteristic result)
          level          (level-of characteristic)]
      (println (maybe-focused characteristic (red (indent (dec level) "- " (.-name characteristic) " (FAILED)"))))
      (flush)))

  (report-error [_this result]
    (println (red (.toString (.-exception result)))))

  (report-runs [_this results]
    (progress/print-summary results)))

(defn ^:export new-documentation-reporter []
  (DocumentationReporter.))

(ns speclj.report.documentation
  (:require [speclj.config :as config]
            [speclj.platform :as platform]
            [speclj.report.progress :as progress]
            [speclj.reporting :refer [green indent red yellow]]))

(defn level-of [component]
  (loop [component @(.-parent component) level 0]
    (if component
      (recur @(.-parent component) (inc level))
      level)))

(defn maybe-focused [text component]
  (cond-> text
          @(.-is-focused? component)
          (str " " (yellow "[FOCUS]"))))

(defn- maybe-profile
  ([text]
   (cond->> text
            config/*profile?*
            (str "           ")))
  ([text result]
   (cond->> text
            config/*profile?*
            (str (yellow (str "[" (platform/format-seconds (.-seconds result)) "s] "))))))

(deftype DocumentationReporter []
  speclj.reporting/Reporter

  (report-message [_this message]
    (println message)
    (flush))

  (report-description [_this description]
    (let [level (level-of description)]
      (when (zero? level) (println))
      (let [output (-> (str (indent level (.-name description)))
                       (maybe-focused description)
                       maybe-profile)]
        (println output)
        (flush))))

  (report-pass [_this result]
    (let [characteristic (.-characteristic result)
          level          (level-of characteristic)
          output         (-> (green (indent (dec level) "- " (.-name characteristic)))
                             (maybe-focused characteristic)
                             (maybe-profile result))]
      (println output)
      (flush)))

  (report-pending [_this result]
    (let [characteristic (.-characteristic result)
          level          (level-of characteristic)
          output         (-> (yellow (indent (dec level) "- " (.-name characteristic) " (PENDING: " (platform/error-message (.-exception result)) ")"))
                             (maybe-profile result))]
      (println output)
      (flush)))

  (report-fail [_this result]
    (let [characteristic (.-characteristic result)
          level          (level-of characteristic)
          output         (-> (red (indent (dec level) "- " (.-name characteristic) " (FAILED)"))
                             (maybe-focused characteristic)
                             (maybe-profile result))]
      (println output)
      (flush)))

  (report-error [_this result]
    (println (red (#?(:cljr .ToString :default .toString) (.-exception result)))))

  (report-runs [_this results]
    (progress/print-summary results)))

(defn ^:export new-documentation-reporter []
  (DocumentationReporter.))

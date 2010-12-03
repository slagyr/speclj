(ns speclj.reporting
  (:use
    [speclj.exec :only (pass? fail?)]
    [speclj.config :only (*reporter* *color?*)]))

(defn failure-source [exception]
  (let [source (nth (.getStackTrace exception) 0)]
    (if-let [filename (.getFileName source)]
      (str (.getCanonicalPath (java.io.File. filename)) ":" (.getLineNumber source))
      "Unknown source")))

(defn tally-time [results]
  (loop [tally 0.0 results results]
    (if (seq results)
      (recur (+ tally (.seconds (first results))) (rest results))
      tally)))

(defprotocol Reporter
  (report-message [reporter message])
  (report-description [this description])
  (report-pass [this result])
  (report-fail [this result])
  (report-runs [this results]))

(defn- stylizer [code]
  (fn [text]
    (if *color?*
      (str "\u001b[" code "m" text "\u001b[0m")
      text)))

(def red (stylizer "31"))
(def green (stylizer "32"))
(def grey (stylizer "90"))

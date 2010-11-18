(ns speclj.reporting
  (:use
    [speclj.exec :only (pass? fail?)]))

(def default-reporter(atom nil))
(declare *reporter*)
(defn active-reporter []
  (if (bound? #'*reporter*)
    *reporter*
    (if-let [reporter @default-reporter]
      reporter
      (throw (Exception. "*reporter* is unbound and no default value has been provided")))))

(defn failure-source [exception]
  (let [source (nth (.getStackTrace exception) 0)]
    (str (.getAbsolutePath (java.io.File. (.getFileName source))) ":" (.getLineNumber source))))

(defn tally-time [results]
  (loop [tally 0.0 results results]
    (if (seq results)
      (recur (+ tally (.seconds (first results))) (rest results))
      tally)))

(defprotocol Reporter
  (report-description [this description])
  (report-pass [this characteristic])
  (report-fail [this characteristic])
  (report-runs [this results]))

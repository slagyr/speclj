(ns speclj.report.silent
  (:use
    [speclj.reporting :only ()])
  (:import
    [speclj.reporting Reporter]))

(deftype SilentReporter [passes fails results]
  Reporter
  (report-description [this description])
  (report-pass [this characteristic])
  (report-fail [this characteristic])
  (report-runs [this results]))

(defn new-silent-reporter []
  (SilentReporter. (atom 0) (atom 0) (atom nil)))
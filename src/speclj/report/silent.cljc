(ns speclj.report.silent
  (:require [speclj.reporting]))

(deftype SilentReporter [passes fails results]
  speclj.reporting/Reporter
  (report-message [_this _message])
  (report-description [_this _description])
  (report-pass [_this _result])
  (report-pending [_this _result])
  (report-fail [_this _result])
  (report-runs [_this _results])
  (report-error [_this _exception]))

(defn ^:export new-silent-reporter []
  (SilentReporter. (atom 0) (atom 0) (atom nil)))

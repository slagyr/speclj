(ns speclj.config-spec
  (:use
    [speclj.core]
    [speclj.config])
  (:require
    [speclj.run.standard]
    [speclj.run.vigilant]
    [speclj.report.progress]
    [speclj.report.silent]))

(describe "Config"
  (it "dynaimcally loads StandardRunner"
    (let [runner (load-runner "standard")]
      (should-not= nil runner)
      (should= speclj.run.standard.StandardRunner (class runner))))

  (it "dynaimcally loads VigilantRunner"
    (let [runner (load-runner "vigilant")]
      (should-not= nil runner)
      (should= speclj.run.vigilant.VigilantRunner (class runner))))

  (it "throws exception with unrecognized runner"
    (should-throw Exception "Failed to load runner: blah" (load-runner "blah")))

  (it "dynaimcally loads ProgressReporter"
    (let [reporter (load-reporter "progress")]
      (should-not= nil reporter)
      (should= speclj.report.progress.ProgressReporter (class reporter))))

  (it "dynaimcally loads SilentReporter"
    (let [reporter (load-reporter "silent")]
      (should-not= nil reporter)
      (should= speclj.report.silent.SilentReporter (class reporter))))

  (it "throws exception with unrecognized reporter"
    (should-throw Exception "Failed to load reporter: blah" (load-reporter "blah")))
  )

(run-specs)

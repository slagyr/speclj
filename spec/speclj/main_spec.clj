(ns speclj.main-spec
  (:use
    [speclj.core]
    [speclj.main])
  (:require
    [speclj.run.standard]
    [speclj.run.vigilant]
    [speclj.report.console]
    [speclj.report.silent]))

(describe "speclj main"

  (it "has default configuration"
    (should= ["spec"] (:spec-dirs default-config))
    (should= "console" (:reporter default-config))
    (should= "standard" (:runner default-config)))

  (it "parses no arguments"
    (should= default-config (parse-args)))

  (it "parses non-option arguments as spec dirs"
    (should= ["one"] (:spec-dirs (parse-args "one")))
    (should= ["one" "two"] (:spec-dirs (parse-args "one" "two")))
    (should= ["one" "two" "three"] (:spec-dirs (parse-args "one" "two" "three"))))

  (it "parses the runner argument"
    (should= "fridge" (:runner (parse-args "--runner=fridge")))
    (should= "freezer" (:runner (parse-args "--runner=freezer"))))

  (it "parses the reporter argument"
    (should= "april" (:reporter (parse-args "--reporter=april")))
    (should= "mary-jane" (:reporter (parse-args "--reporter=mary-jane"))))

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

  (it "dynaimcally loads ConsoleReporter"
    (let [reporter (load-reporter "console")]
      (should-not= nil reporter)
      (should= speclj.report.console.ConsoleReporter (class reporter))))

  (it "dynaimcally loads SilentReporter"
    (let [reporter (load-reporter "silent")]
      (should-not= nil reporter)
      (should= speclj.report.silent.SilentReporter (class reporter))))

  (it "throws exception with unrecognized reporter"
    (should-throw Exception "Failed to load reporter: blah" (load-reporter "blah")))
  )

(conclude-single-file-run)
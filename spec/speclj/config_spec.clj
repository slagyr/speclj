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

  (it "converts tag input to includes/excludes"
    (should= {:includes #{} :excludes #{}} (parse-tags []))
    (should= {:includes #{:one} :excludes #{}} (parse-tags [:one]))
    (should= {:includes #{:one} :excludes #{}} (parse-tags ["one"]))
    (should= {:includes #{:one :two} :excludes #{}} (parse-tags ["one" 'two]))
    (should= {:includes #{} :excludes #{:one}} (parse-tags ["~one"]))
    (should= {:includes #{} :excludes #{:one :two}} (parse-tags ["~one" "~two"]))
    (should= {:includes #{:two} :excludes #{:one}} (parse-tags ["~one" "two"])))

  (it "should translate tags in config-bindings"
    (let [mappings (config-mappings (assoc default-config :tags ["one" "~two"]))]
      (should=
        {:includes #{:one} :excludes #{:two}}
        (get mappings #'*tag-filter*))))

  )

(run-specs)

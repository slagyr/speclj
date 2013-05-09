(ns speclj.config-spec
  (:require ;cljs-macros
            [speclj.core :refer [describe it should-not= should= should-throw should-not-contain]]
            [speclj.platform])
  (:require [speclj.config :refer [load-runner load-reporter default-config
                                   parse-tags config-mappings *tag-filter* config-bindings]]
            [speclj.platform :refer [exception]]
            [speclj.report.progress]
            [speclj.report.silent]
            [speclj.run.standard :refer [run-specs]]))

(describe "Config"
  (it "dynaimcally loads StandardRunner"
    (let [runner (load-runner "standard")]
      (should-not= nil runner)
      (should= speclj.run.standard.StandardRunner (type runner))))

  ;cljs-ignore->
  (it "dynaimcally loads VigilantRunner"
    (let [runner (load-runner "vigilant")]
      (should-not= nil runner)
      (should= "speclj.run.vigilant.VigilantRunner" (.getName (type runner)))))
  ;<-cljs-ignore

  (it "throws exception with unrecognized runner"
    (should-throw exception "Failed to load runner: blah" (load-runner "blah")))

  (it "dynaimcally loads ProgressReporter"
    (let [reporter (load-reporter "progress")]
      (should-not= nil reporter)
      (should= speclj.report.progress.ProgressReporter (type reporter))))

  (it "dynaimcally loads SilentReporter"
    (let [reporter (load-reporter "silent")]
      (should-not= nil reporter)
      (should= speclj.report.silent.SilentReporter (type reporter))))

  (it "throws exception with unrecognized reporter"
    (should-throw exception "Failed to load reporter: blah" (load-reporter "blah")))

  (it "converts tag input to includes/excludes"
    (should= {:includes #{} :excludes #{}} (parse-tags []))
    (should= {:includes #{:one} :excludes #{}} (parse-tags [:one]))
    (should= {:includes #{:one} :excludes #{}} (parse-tags ["one"]))
    (should= {:includes #{:one :two} :excludes #{}} (parse-tags ["one" 'two]))
    (should= {:includes #{} :excludes #{:one}} (parse-tags ["~one"]))
    (should= {:includes #{} :excludes #{:one :two}} (parse-tags ["~one" "~two"]))
    (should= {:includes #{:two} :excludes #{:one}} (parse-tags ["~one" "two"])))

  ;cljs-ignore->
  (it "should translate tags in config-bindings"
    (let [mappings (config-mappings (assoc default-config :tags ["one" "~two"]))]
      (should=
        {:includes #{:one} :excludes #{:two}}
        (get mappings #'*tag-filter*))))

  (it "doesn't include *parent-description* in config-bindings"
    (let [cb (config-bindings)]
      (should-not-contain #'speclj.config/*parent-description* cb)))
  ;<-cljs-ignore

  )

(run-specs)

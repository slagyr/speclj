(ns speclj.config-spec
  (#?(:cljs :require-macros :clj :require)
    [speclj.core :refer [describe it should-not= should= should-throw should-not-contain should-be-same]])
  (:require [speclj.config :refer [load-runner load-reporter default-config
                                   parse-tags config-mappings *tag-filter* config-bindings]]
            [speclj.platform :as platform]
            [speclj.report.progress]
            [speclj.report.silent]
            [speclj.run.standard :refer [run-specs]]))


(describe "Config"
  (it "dynamically loads StandardRunner"
    (let [runner (load-runner "standard")]
      (should-not= nil runner)
      (should= speclj.run.standard.StandardRunner (type runner))))

  #?(:clj
     (it "dynamically loads VigilantRunner"
       (let [runner (load-runner "vigilant")]
         (should-not= nil runner)
         (should= "speclj.run.vigilant.VigilantRunner" (.getName (type runner))))))

  (it "throws exception with unrecognized runner"
    (should-throw platform/exception "Failed to load runner: blah" (load-runner "blah"))
    )

  (it "dynamically loads ProgressReporter"
    (let [reporter (load-reporter "progress")]
      (should-not= nil reporter)
      (should= speclj.report.progress.ProgressReporter (type reporter))))

  (it "dynamically loads SilentReporter"
    (let [reporter (load-reporter "silent")]
      (should-not= nil reporter)
      (should= speclj.report.silent.SilentReporter (type reporter))))

  (it "throws exception with unrecognized reporter"
    (should-throw platform/exception "Failed to load reporter: blah" (load-reporter "blah")))

  (it "can be given a pre-fabricated reporter"
    (let [pre-fabricated-reporter (speclj.report.silent/new-silent-reporter)
          reporter (load-reporter pre-fabricated-reporter)]
      (should-not= nil reporter)
      (should-be-same reporter pre-fabricated-reporter)))

  (it "converts tag input to includes/excludes"
    (should= {:includes #{} :excludes #{}} (parse-tags []))
    (should= {:includes #{:one} :excludes #{}} (parse-tags [:one]))
    (should= {:includes #{:one} :excludes #{}} (parse-tags ["one"]))
    (should= {:includes #{:one :two} :excludes #{}} (parse-tags ["one" 'two]))
    (should= {:includes #{} :excludes #{:one}} (parse-tags ["~one"]))
    (should= {:includes #{} :excludes #{:one :two}} (parse-tags ["~one" "~two"]))
    (should= {:includes #{:two} :excludes #{:one}} (parse-tags ["~one" "two"])))

  #?(:clj
     (it "should translate tags in config-bindings"
       (let [mappings (config-mappings (assoc default-config :tags ["one" "~two"]))]
         (should=
           {:includes #{:one} :excludes #{:two}}
           (get mappings #'*tag-filter*)))))
  #?(:clj
     (it "doesn't include *parent-description* in config-bindings"
       (let [cb (config-bindings)]
         (should-not-contain #'speclj.config/*parent-description* cb))))

  )

(run-specs)



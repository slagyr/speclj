(ns speclj.config-spec
  (:require [speclj.core #?(:clj :refer :cljs :refer-macros) [describe context it should-not= should= should-throw should-not-contain should-not-be-nil should-be-same]]
            [speclj.spec-helper #?(:clj :refer :cljs :refer-macros) [test-exported-meta]]
            [speclj.config :as sut]
            [speclj.platform :as platform]
            [speclj.report.progress]
            [speclj.report.silent]
            [speclj.run.standard :as standard]))

(describe "Config"
  (it "dynamically loads StandardRunner"
    (let [runner (sut/load-runner "standard")]
      (should-not= nil runner)
      (should= speclj.run.standard.StandardRunner (type runner))))

  #?(:clj
     (it "dynamically loads VigilantRunner"
       (let [runner (sut/load-runner "vigilant")]
         (should-not= nil runner)
         (should= "speclj.run.vigilant.VigilantRunner" (.getName (type runner))))))

  (it "throws exception with unrecognized runner"
    (should-throw platform/exception "Failed to load runner: blah" (sut/load-runner "blah")))

  (it "dynamically loads ProgressReporter"
    (let [reporter (sut/load-reporter "progress")]
      (should-not= nil reporter)
      (should= speclj.report.progress.ProgressReporter (type reporter))))

  (it "dynamically loads SilentReporter"
    (let [reporter (sut/load-reporter "silent")]
      (should-not-be-nil reporter)
      (should= speclj.report.silent.SilentReporter (type reporter))))

  (it "throws exception with unrecognized reporter"
    (should-throw platform/exception "Failed to load reporter: blah" (sut/load-reporter "blah")))

  (it "can be given a pre-fabricated reporter"
    (let [pre-fabricated-reporter (speclj.report.silent/new-silent-reporter)
          reporter                (sut/load-reporter pre-fabricated-reporter)]
      (should-not= nil reporter)
      (should-be-same reporter pre-fabricated-reporter)))

  (it "converts tag input to includes/excludes"
    (should= {:includes #{} :excludes #{}} (sut/parse-tags []))
    (should= {:includes #{:one} :excludes #{}} (sut/parse-tags [:one]))
    (should= {:includes #{:one} :excludes #{}} (sut/parse-tags ["one"]))
    (should= {:includes #{:one :two} :excludes #{}} (sut/parse-tags ["one" 'two]))
    (should= {:includes #{} :excludes #{:one}} (sut/parse-tags ["~one"]))
    (should= {:includes #{} :excludes #{:one :two}} (sut/parse-tags ["~one" "~two"]))
    (should= {:includes #{:two} :excludes #{:one}} (sut/parse-tags ["~one" "two"])))

  #?(:clj
     (it "should translate tags in config-bindings"
       (let [mappings (sut/config-mappings (assoc sut/default-config :tags ["one" "~two"]))]
         (should=
           {:includes #{:one} :excludes #{:two}}
           (get mappings #'sut/*tag-filter*)))))
  #?(:clj
     (it "doesn't include *parent-description* in config-bindings"
       (let [cb (sut/config-bindings)]
         (should-not-contain #'speclj.config/*parent-description* cb))))

  (context "exporting"
    (test-exported-meta sut/active-runner)
    )
  )

(standard/run-specs)

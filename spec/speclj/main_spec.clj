(ns speclj.main-spec
  (:use
    [speclj.core]
    [speclj.main]
    [speclj.util :only (endl)])
  (:require
    [speclj.run.standard]
    [speclj.run.vigilant]
    [speclj.report.progress]
    [speclj.report.silent]
    [speclj.version]
    [speclj.config :as config])
  (:import
    [java.io ByteArrayOutputStream OutputStreamWriter]))


(defn to-s [output]
  (String. (.toByteArray output)))

(describe "speclj main"
  (with output (ByteArrayOutputStream.))
  (with writer (OutputStreamWriter. @output))
  (around [spec] (binding [*out* @writer] (spec)))
  (around [spec] (binding [exit identity] (spec)))

  (it "has default configuration"
    (should= ["spec"] (:specs default-config))
    (should= "progress" (:reporter default-config))
    (should= "standard" (:runner default-config)))

  (it "parses no arguments"
    (should= default-config (parse-args)))

  (it "parses non-option arguments as spec dirs"
    (should= ["one"] (:specs (parse-args "one")))
    (should= ["one" "two"] (:specs (parse-args "one" "two")))
    (should= ["one" "two" "three"] (:specs (parse-args "one" "two" "three"))))

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

  (it "uses formatter as an alias to reporter"
    (let [options (parse-args "--format" "silent")]
      (should= "silent" (:reporter options))))      

  (it "parses the --version switch"
    (should= nil (:version (parse-args "")))
    (should= "on" (:version (parse-args "--version")))
    (should= "on" (:version (parse-args "-v"))))

  (it "handles the --version switch"
    (should= 0 (run "--version"))
    (should= (str "speclj " speclj.version/string endl) (to-s @output)))

  (it "parses the --help switch"
    (should= nil (:help (parse-args "")))
    (should= "on" (:help (parse-args "--help")))
    (should= "on" (:help (parse-args "-h"))))

  (it "handles the --version switch"
    (should= 0 (run "--help"))
    (should-not= -1 (.indexOf (to-s @output) "Usage")))

  (it "parses and translates the --autotest option"
    (let [options (parse-args "--autotest")]
      (should= "vigilant" (:runner options))
      (should= "specdoc" (:reporter options)))
    (let [options (parse-args "-a")]
      (should= "vigilant" (:runner options))
      (should= "specdoc" (:reporter options))))

  (it "parses the --color switch"
    (should= nil (:color (parse-args "")))
    (should= "on" (:color (parse-args "--color")))
    (should= "on" (:color (parse-args "-c"))))

  (it "builds var mappings from config"
    (with-bindings (config-mappings {:runner "standard" :reporter "progress" :color true})
      (should= true config/*color?*)))
          
  )

(run-specs)
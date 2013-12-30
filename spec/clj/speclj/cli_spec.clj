(ns speclj.cli-spec
  (:require [speclj.config :as config]
            [speclj.core :refer :all]
            [speclj.cli :refer :all]
            [speclj.platform :refer [endl]]
            [speclj.version])
  (:import [java.io ByteArrayOutputStream OutputStreamWriter]))

(defn to-s [output]
  (String. (.toByteArray output)))

(describe "speclj.cli"
  (with output (ByteArrayOutputStream.))
  (with writer (OutputStreamWriter. @output))
  (around [spec] (binding [*out* @writer] (spec)))

  (it "has default configuration"
    (should= ["spec"] (:specs config/default-config))
    (should= ["progress"] (:reporters config/default-config))
    (should= "standard" (:runner config/default-config)))

  (it "parses no arguments"
    (should= config/default-config (parse-args)))

  (it "parses non-option arguments as spec dirs"
    (should= ["one"] (:specs (parse-args "one")))
    (should= ["one" "two"] (:specs (parse-args "one" "two")))
    (should= ["one" "two" "three"] (:specs (parse-args "one" "two" "three"))))

  (it "parses the runner argument"
    (should= "fridge" (:runner (parse-args "--runner=fridge")))
    (should= "freezer" (:runner (parse-args "-r" "freezer"))))

  (it "parses the reporter argument"
    (should= ["april"] (:reporters (parse-args "--reporter=april")))
    (should= ["mary-jane"] (:reporters (parse-args "-f" "mary-jane")))
    (should= ["april" "mj"] (:reporters (parse-args "--reporter=april" "--reporter=mj")))
    (should= ["mj" "april"] (:reporters (parse-args "-f" "mj" "-f" "april"))))

  (it "uses formatter as an alias to reporter"
    (should= ["silent"] (:reporters (parse-args "--format" "silent")))
    (should= ["silent" "progress"] (:reporters (parse-args "--format=silent" "--format=progress"))))

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
      (should= ["documentation"] (:reporters options)))
    (let [options (parse-args "-a")]
      (should= "vigilant" (:runner options))
      (should= ["documentation"] (:reporters options))))

  (it "allows adding extra reporters when using autotest"
    (let [options (parse-args "-a" "-f" "progress")]
      (should= "vigilant" (:runner options))
      (should= ["documentation" "progress"] (sort (:reporters options)))))

  (it "parses the --color switch"
    (should= nil (:color (parse-args "")))
    (should= "on" (:color (parse-args "--color")))
    (should= "on" (:color (parse-args "-c"))))

  (it "parses the --no-color switch"
      (should= nil (:color (parse-args "-C")))
      (should= nil (:color (parse-args "-c" "-C"))))

  (it "builds var mappings from config"
    (with-bindings (config/config-mappings {:runner "standard" :reporter "progress" :color true})
      (should= true config/*color?*)))

  (it "parses the --stacktrace switch"
    (should= nil (:stacktrace (parse-args "")))
    (should= "on" (:stacktrace (parse-args "--stacktrace")))
    (should= "on" (:stacktrace (parse-args "-b"))))

  (it "set stacktrace in config"
    (with-bindings (config/config-mappings {:runner "standard" :reporter "progress"})
      (should= false config/*full-stack-trace?*))
    (with-bindings (config/config-mappings {:runner "standard" :reporter "progress" :stacktrace true})
      (should= true config/*full-stack-trace?*)))

  (it "resolves reporter aliases"
    (should= ["silent"] (:reporters (parse-args "-f" "s")))
    (should= ["progress"] (:reporters (parse-args "-f" "p")))
    (should= ["documentation"] (:reporters (parse-args "-f" "d")))
    (should= ["clojure-test"] (:reporters (parse-args "-f" "c"))))

  (it "resolves runner aliases"
    (should= "standard" (:runner (parse-args "-r" "s")))
    (should= "vigilant" (:runner (parse-args "-r" "v"))))

  (it "parses the --tag option"
    (should= ["one"] (:tags (parse-args "--tag=one")))
    (should= ["one"] (:tags (parse-args "-t" "one")))
    (should= ["one" "~two"] (:tags (parse-args "--tag=one" "--tag=~two")))
    (should= ["one" "~two"] (:tags (parse-args "-t" "one" "-t" "~two"))))

  )

(run-specs)

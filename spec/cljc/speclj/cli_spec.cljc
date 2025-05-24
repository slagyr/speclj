(ns speclj.cli-spec
  (:require [speclj.cli :as sut]
            [speclj.config :as config]
            [speclj.core #?(:cljs :refer-macros :default :refer) [describe it should= should-contain should-not-be-nil]]
            [speclj.platform :refer [endl]]
            [clojure.string :as str]
            #?(:clj [trptcolin.versioneer.core :as version])))

(describe "speclj.cli"

  (it "has default configuration"
    (should= ["spec"] (:specs config/default-config))
    (should= ["progress"] (:reporters config/default-config))
    (should= "standard" (:runner config/default-config)))

  (it "parses no arguments"
    (should= config/default-config (sut/parse-args)))

  (it "parses non-option arguments as spec dirs"
    (should= ["one"] (:specs (sut/parse-args "one")))
    (should= ["one" "two"] (:specs (sut/parse-args "one" "two")))
    (should= ["one" "two" "three"] (:specs (sut/parse-args "one" "two" "three"))))

  (it "parses the runner argument"
    (should= "fridge" (:runner (sut/parse-args "--runner=fridge")))
    (should= "freezer" (:runner (sut/parse-args "-r" "freezer"))))

  (it "parses the reporter argument"
    (should= ["april"] (:reporters (sut/parse-args "--reporter=april")))
    (should= ["mary-jane"] (:reporters (sut/parse-args "-f" "mary-jane")))
    (should= ["april" "mj"] (:reporters (sut/parse-args "--reporter=april" "--reporter=mj")))
    (should= ["mj" "april"] (:reporters (sut/parse-args "-f" "mj" "-f" "april"))))

  (it "uses default-spec-dirs argument as a default for specs"
    (should= ["april"] (:specs (sut/parse-args "--default-spec-dirs=april")))
    (should= ["mary-jane"] (:specs (sut/parse-args "-D" "mary-jane")))
    (should= ["april" "mj"] (:specs (sut/parse-args "--default-spec-dirs=april" "--default-spec-dirs=mj")))
    (should= ["mj" "april"] (:specs (sut/parse-args "-D" "mj" "-D" "april")))
    (should= ["foo/bar/baz"] (:specs (sut/parse-args "--default-spec-dirs=april" "foo/bar/baz"))))

  (it "uses formatter as an alias to reporter"
    (should= ["silent"] (:reporters (sut/parse-args "--format" "silent")))
    (should= ["silent" "progress"] (:reporters (sut/parse-args "--format=silent" "--format=progress"))))

  (it "parses the --version switch"
    (should= nil (:version (sut/parse-args "")))
    (should= "on" (:version (sut/parse-args "--version")))
    (should= "on" (:version (sut/parse-args "-v"))))

  (it "handles the --version switch"
    (let [version #?(:clj (version/get-version "speclj" "speclj") :default "")
          result          (atom nil)
          out             (with-out-str (reset! result (sut/run "--version")))]
      (should= 0 @result)
      (should= (str "speclj " version endl) out)))

  (it "parses the --help switch"
    (should= nil (:help (sut/parse-args "")))
    (should= "on" (:help (sut/parse-args "--help")))
    (should= "on" (:help (sut/parse-args "-h"))))

  (it "handles the --help switch"
    (let [result (atom nil)
          out    (with-out-str (reset! result (sut/run "--help")))]
      (should= 0 @result)
      (should-not-be-nil (str/index-of out "Usage"))))

  (it "includes the profile switch"
    (let [out (with-out-str (sut/run "--help"))]
      (should-contain "-P, --profile" out)
      (should-contain "Shows execution time for each test." out)))

  (it "parses the --speclj switch"
    (should= nil (:speclj (sut/parse-args "")))
    (should= "on" (:speclj (sut/parse-args "--speclj")))
    (should= "on" (:speclj (sut/parse-args "-S"))))

  (it "handles the --speclj switch"
    (let [result (atom nil)
          out    (with-out-str (reset! result (sut/run "--speclj")))]
      (should= 0 @result)
      (should-not-be-nil (str/index-of out "Usage"))))

  (it "parses and translates the --autotest option"
    (let [options (sut/parse-args "--autotest")]
      (should= "vigilant" (:runner options))
      (should= ["documentation"] (:reporters options)))
    (let [options (sut/parse-args "-a")]
      (should= "vigilant" (:runner options))
      (should= ["documentation"] (:reporters options))))

  (it "allows adding extra reporters when using autotest"
    (let [options (sut/parse-args "-a" "-f" "progress")]
      (should= "vigilant" (:runner options))
      (should= ["documentation" "progress"] (sort (:reporters options)))))

  (it "parses the --color switch"
    (should= nil (:color (sut/parse-args "")))
    (should= "on" (:color (sut/parse-args "--color")))
    (should= "on" (:color (sut/parse-args "-c"))))

  (it "parses the --no-color switch"
    (should= nil (:color (sut/parse-args "-C")))
    (should= nil (:color (sut/parse-args "-c" "-C"))))

  (it "parses the --omit-pending option"
    (should= false (:omit-pending (sut/parse-args "")))
    (should= "on" (:omit-pending (sut/parse-args "-p")))
    (should= "on" (:omit-pending (sut/parse-args "--omit-pending"))))

  (it "builds var mappings from config"
    (config/with-config {:runner "standard" :reporter "progress" :color true}
      #(should= true config/*color?*)))

  (it "parses the --stacktrace switch"
    (should= nil (:stacktrace (sut/parse-args "")))
    (should= "on" (:stacktrace (sut/parse-args "--stacktrace")))
    (should= "on" (:stacktrace (sut/parse-args "-b"))))

  (it "set stacktrace in config"
    (config/with-config {:runner "standard" :reporter "progress"}
      #(should= false config/*full-stack-trace?*))
    (config/with-config {:runner "standard" :reporter "progress" :stacktrace true}
      #(should= true config/*full-stack-trace?*)))

  (it "resolves reporter aliases"
    (should= ["silent"] (:reporters (sut/parse-args "-f" "s")))
    (should= ["progress"] (:reporters (sut/parse-args "-f" "p")))
    (should= ["documentation"] (:reporters (sut/parse-args "-f" "d")))
    (should= ["clojure-test"] (:reporters (sut/parse-args "-f" "c"))))

  (it "resolves runner aliases"
    (should= "standard" (:runner (sut/parse-args "-r" "s")))
    (should= "vigilant" (:runner (sut/parse-args "-r" "v"))))

  (it "parses the --tag option"
    (should= ["one"] (:tags (sut/parse-args "--tag=one")))
    (should= ["one"] (:tags (sut/parse-args "-t" "one")))
    (should= ["one" "~two"] (:tags (sut/parse-args "--tag=one" "--tag=~two")))
    (should= ["one" "~two"] (:tags (sut/parse-args "-t" "one" "-t" "~two"))))
  )

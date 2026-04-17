(ns speclj.cli-spec
  (:require [speclj.cli :as sut]
            [speclj.config :as config]
            [speclj.core #?(:cljs :refer-macros :default :refer) [context describe it should= should-contain should-not-be-nil]]
            [speclj.platform :refer [endl]]
            [clojure.string :as str]))

(describe "CLI"

  (it "has default configuration"
    (should= ["src"] (:sources config/default-config))
    (should= ["spec"] (:specs config/default-config))
    (should= ["progress"] (:reporters config/default-config))
    (should= "standard" (:runner config/default-config)))

  (context "argument parsing"

    (it "parses non-option arguments as spec dirs"
      (should= ["one"] (:specs (sut/parse-args "one")))
      (should= ["one" "two"] (:specs (sut/parse-args "one" "two")))
      (should= ["one" "two" "three"] (:specs (sut/parse-args "one" "two" "three"))))

    (it "parses the runner argument"
      (should= "fridge" (:runner (sut/parse-args "--runner=fridge")))
      (should= "freezer" (:runner (sut/parse-args "-r" "freezer"))))

    (it "parses the runner argument"
      (should= "fridge" (:runner (sut/parse-args "--runner=fridge")))
      (should= "freezer" (:runner (sut/parse-args "-r" "freezer"))))

    (it "parses the reporter argument"
      (should= ["april"] (:reporters (sut/parse-args "--reporter=april")))
      (should= ["mary-jane"] (:reporters (sut/parse-args "-f" "mary-jane")))
      (should= ["april" "mj"] (:reporters (sut/parse-args "--reporter=april" "--reporter=mj")))
      (should= ["mj" "april"] (:reporters (sut/parse-args "-f" "mj" "-f" "april"))))

    (it "uses --default argument as a default for specs"
      (should= ["april"] (:specs (sut/parse-args "--default=april")))
      (should= ["mary-jane"] (:specs (sut/parse-args "-D" "mary-jane")))
      (should= ["april" "mj"] (:specs (sut/parse-args "--default=april" "--default=mj")))
      (should= ["mj" "april"] (:specs (sut/parse-args "-D" "mj" "-D" "april")))
      (should= ["foo/bar/baz"] (:specs (sut/parse-args "--default=april" "foo/bar/baz"))))

    (it "uses formatter as an alias to reporter"
      (should= ["silent"] (:reporters (sut/parse-args "--format" "silent")))
      (should= ["silent" "progress"] (:reporters (sut/parse-args "--format=silent" "--format=progress"))))

    (it "parses the --version switch"
      (should= nil (:version (sut/parse-args "")))
      (should= "on" (:version (sut/parse-args "--version")))
      (should= "on" (:version (sut/parse-args "-v"))))

    (it "handles the --version switch"
      (let [result (atom nil)
            out    (with-out-str (reset! result (sut/run "--version")))]
        (should= 0 @result)
        (should= (str "speclj " (sut/get-version) endl) out)))

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
        (should-contain "Shows execution time for each test (documentation reporter)." out)))

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

    (it "parses the --sources option"
      (should= ["src/one"] (:sources (sut/parse-args "--sources=src/one")))
      (should= ["src/two"] (:sources (sut/parse-args "-s=src/two")))
      (should= ["src/one" "src/two"] (:sources (sut/parse-args "--sources=src/one" "--sources=src/two")))
      (should= ["src/one" "src/two"] (:sources (sut/parse-args "-s" "src/one" "--sources" "src/two")))
      (should= ["src/one" "src/two"] (:sources (sut/parse-args "--sources" "src/one" "-s" "src/two"))))

    (context "file:line targets"

      (it "splits a single file:line target"
        (let [options (sut/parse-args "spec/foo_spec.clj:42")]
          (should= ["spec/foo_spec.clj"] (:specs options))
          (should= {"spec/foo_spec.clj" 42} (:line-targets options))))

      (it "leaves bare paths alone"
        (let [options (sut/parse-args "spec/foo_spec.clj")]
          (should= ["spec/foo_spec.clj"] (:specs options))
          (should= nil (:line-targets options))))

      (it "leaves directory paths alone"
        (let [options (sut/parse-args "spec")]
          (should= ["spec"] (:specs options))
          (should= nil (:line-targets options))))

      (it "preserves Windows-style drive letters"
        (let [options (sut/parse-args "C:\\spec\\foo.clj:42")]
          (should= ["C:\\spec\\foo.clj"] (:specs options))
          (should= {"C:\\spec\\foo.clj" 42} (:line-targets options))))

      (it "does not parse non-numeric :suffixes as line numbers"
        (let [options (sut/parse-args "foo.clj:bar")]
          (should= ["foo.clj:bar"] (:specs options))
          (should= nil (:line-targets options))))

      (it "keeps multiple targets when no bare dir covers them"
        (let [options (sut/parse-args "a.clj:10" "b.clj:20")]
          (should= ["a.clj" "b.clj"] (:specs options))
          (should= {"a.clj" 10 "b.clj" 20} (:line-targets options))))

      #?(:clj
         (it "drops file:line targets covered by a bare directory arg"
           (let [options (sut/parse-args "spec" "spec/cljc/speclj/cli_spec.cljc:42")]
             (should= ["spec" "spec/cljc/speclj/cli_spec.cljc"] (:specs options))
             (should= nil (:line-targets options)))))

      #?(:clj
         (it "keeps file:line targets outside any bare directory arg"
           (let [options (sut/parse-args "examples" "spec/cljc/speclj/cli_spec.cljc:42")]
             (should= ["examples" "spec/cljc/speclj/cli_spec.cljc"] (:specs options))
             (should= {"spec/cljc/speclj/cli_spec.cljc" 42} (:line-targets options)))))

      (it "binds *line-targets* through with-config"
        (config/with-config {:runner "standard" :reporter "progress"
                             :line-targets {"foo.clj" 42}}
          #(should= {"foo.clj" 42} config/*line-targets*)))
      )

    (context "--focus"

      (it "overrides other specs args"
        (let [options (sut/parse-args "spec" "other/dir" "--focus" "spec/foo.clj:42")]
          (should= ["spec/foo.clj"] (:specs options))
          (should= {"spec/foo.clj" 42} (:line-targets options))))

      (it "accepts a bare directory"
        (let [options (sut/parse-args "--focus" "some/dir")]
          (should= ["some/dir"] (:specs options))
          (should= nil (:line-targets options))))

      (it "ignores default-spec-dirs from wrappers"
        (let [options (sut/parse-args "-D" "spec/cljc" "-D" "spec/clj" "--focus" "only.clj:5")]
          (should= ["only.clj"] (:specs options))
          (should= {"only.clj" 5} (:line-targets options))))

      (it "accepts the -F short form"
        (let [options (sut/parse-args "-F" "only.clj:5")]
          (should= ["only.clj"] (:specs options))
          (should= {"only.clj" 5} (:line-targets options))))

      (it "is repeatable — unions multiple --focus values"
        (let [options (sut/parse-args "-F" "a.clj:10" "-F" "b.clj:20")]
          (should= ["a.clj" "b.clj"] (:specs options))
          (should= {"a.clj" 10 "b.clj" 20} (:line-targets options))))

      #?(:clj
         (it "prunes file:line targets covered by a --focus dir"
           (let [options (sut/parse-args "-F" "examples" "-F" "examples/focus/focus.clj:5")]
             (should= ["examples" "examples/focus/focus.clj"] (:specs options))
             (should= nil (:line-targets options)))))

      #?(:clj
         (it "combines a dir focus with a sibling file:line focus"
           (let [options (sut/parse-args "-F" "examples" "-F" "spec/cljc/speclj/cli_spec.cljc:11")]
             (should= ["examples" "spec/cljc/speclj/cli_spec.cljc"] (:specs options))
             (should= {"spec/cljc/speclj/cli_spec.cljc" 11} (:line-targets options))))))
    )
  )

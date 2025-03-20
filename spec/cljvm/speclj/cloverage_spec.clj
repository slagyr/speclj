(ns speclj.cloverage-spec
  (:require [cloverage.args :as args]
            [cloverage.coverage :as coverage]
            [cloverage.coverage]
            [speclj.cli :as cli]
            [speclj.cloverage :as sut]
            [speclj.core :refer :all]
            [speclj.stub :as stub]))

(defmacro speclj-opts-should= [options]
  `(let [[_# speclj-opts#] (stub/last-invocation-of :coverage/run-main)]
     (should= ~options speclj-opts#)))

(defmacro cloverage-opts-should= [options]
  `(let [[cloverage-opts# _#] (stub/last-invocation-of :coverage/run-main)]
     (should= ~options cloverage-opts#)))

(defn run-cloverage [runner-opts]
  (let [runner (coverage/runner-fn {:runner :speclj :runner-opts runner-opts})]
    (runner nil)))

(describe "Cloverage"
  (with-stubs)

  (context "-main"

    (redefs-around [coverage/run-main (stub :coverage/run-main)])

    (it "no arguments"
      (let [speclj-opts    {:runner-opts {:args []}}
            cloverage-opts (args/parse-args ["-r" ":speclj"] speclj-opts)]
        (sut/-main)
        (speclj-opts-should= speclj-opts)
        (cloverage-opts-should= cloverage-opts)))

    (it "with color"
      (let [speclj-opts    {:runner-opts {:args ["-c"]}}
            cloverage-opts (args/parse-args ["-r" ":speclj"] speclj-opts)]
        (sut/-main "-c")
        (speclj-opts-should= speclj-opts)
        (cloverage-opts-should= cloverage-opts)))

    (it "with color and without slow specs"
      (let [speclj-opts    {:runner-opts {:args ["-c" "-t" "~slow"]}}
            cloverage-opts (args/parse-args ["-r" ":speclj"] speclj-opts)]
        (sut/-main "-c" "-t" "~slow")
        (speclj-opts-should= speclj-opts)
        (cloverage-opts-should= cloverage-opts)))

    (it "ends with a double hyphen"
      (let [speclj-opts    {:runner-opts {:args ["-c" "-t" "~slow"]}}
            cloverage-opts (args/parse-args ["-r" ":speclj"] speclj-opts)]
        (sut/-main "-c" "-t" "~slow" "--")
        (speclj-opts-should= speclj-opts)
        (cloverage-opts-should= cloverage-opts)))

    (it "only argument is a double hyphen"
      (let [speclj-opts    {:runner-opts {:args []}}
            cloverage-opts (args/parse-args ["-r" ":speclj"] speclj-opts)]
        (sut/-main "--")
        (speclj-opts-should= speclj-opts)
        (cloverage-opts-should= cloverage-opts)))

    (it "many double hyphens"
      (let [speclj-opts    {:runner-opts {:args []}}
            cloverage-opts (args/parse-args ["-r" ":speclj" "--" "--"] speclj-opts)]
        (sut/-main "--" "--" "--")
        (speclj-opts-should= speclj-opts)
        (cloverage-opts-should= cloverage-opts)))

    (it "passes arguments to cloverage and not speclj"
      (let [speclj-opts    {:runner-opts {:args []}}
            cloverage-opts (args/parse-args ["-r" ":speclj" "-p" "src/clj" "-s" "spec/clj"] speclj-opts)]
        (sut/-main "--" "-p" "src/clj" "-s" "spec/clj")
        (speclj-opts-should= speclj-opts)
        (cloverage-opts-should= cloverage-opts)))

    (it "passes arguments to both cloverage and speclj"
      (let [speclj-opts    {:runner-opts {:args ["-c" "-t" "~slow"]}}
            cloverage-opts (args/parse-args ["-r" ":speclj" "-p" "src/clj" "-s" "spec/clj"] speclj-opts)]
        (sut/-main "-c" "-t" "~slow" "--" "-p" "src/clj" "-s" "spec/clj")
        (speclj-opts-should= speclj-opts)
        (cloverage-opts-should= cloverage-opts)))

    )

  (context "coverage runner-fn"

    (redefs-around [cli/run (stub :cli/run {:return 0})])

    (it "creates a function"
      (let [run-specs (coverage/runner-fn {:runner :speclj})]
        (should-be fn? run-specs)
        (should-not-have-invoked :cli/run)))

    (it "no runner options"
      (let [run-specs (coverage/runner-fn {:runner :speclj})]
        (should= {:errors 0} (run-specs nil))
        (should-have-invoked :cli/run {:with []})))

    (it "empty runner options"
      (let [result (run-cloverage [])]
        (should= {:errors 0} result)
        (should-have-invoked :cli/run {:with []})))

    (it "runner options do not contain args"
      (let [result (run-cloverage [["foo" "bar"]])]
        (should= {:errors 0} result)
        (should-have-invoked :cli/run {:with []})))

    (it "runner options with empty arguments"
      (let [result (run-cloverage [[:args []]])]
        (should= {:errors 0} result)
        (should-have-invoked :cli/run {:with []})))

    (it "runner options with one argument"
      (let [result (run-cloverage [[:args ["-c"]]])]
        (should= {:errors 0} result)
        (should-have-invoked :cli/run {:with ["-c"]})))

    (it "runner options with many arguments"
      (let [result (run-cloverage [[:args ["-c" "-t" "~slow"]]])]
        (should= {:errors 0} result)
        (should-have-invoked :cli/run {:with ["-c" "-t" "~slow"]})))

    (it "runner options pick up some garbage"
      (let [result (run-cloverage [["foo" "bar"]
                               [:args ["-c" "-t" "~slow"]]
                               ["baz" "buzz"]])]
        (should= {:errors 0} result)
        (should-have-invoked :cli/run {:with ["-c" "-t" "~slow"]})))

    (it "results in some failures"
      (with-redefs [cli/run (constantly 5)]
        (let [result (run-cloverage [[:args ["-c" "-t" "~slow"]]])]
          (should= {:errors 5} result))))
    )
  )

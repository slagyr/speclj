(ns speclj.run.standard-spec
  (:require [speclj.components :as components]
            [speclj.config :as config]
            [speclj.core :refer :all]
            [speclj.freshener :as fresh]
            [speclj.io :as io]
            [speclj.line-filter :as line-filter]
            [speclj.report.silent :as silent]
            [speclj.run.standard :as sut]
            [speclj.running :as running]
            [speclj.spec-helper :as spec-helper]
            [speclj.stub :as stub]))

(defn find-dir
  ([name] (find-dir (io/as-file (io/canonical-path (io/as-file "."))) name))
  ([file name]
   (let [examples (io/as-file file name)]
     (if (io/exists? examples)
       examples
       (find-dir (io/parent-file file) name)))))

(def examples-dir (find-dir "examples"))
(def prime-factors-dir (io/canonical-path (io/as-file examples-dir "prime_factors")))
(def failures-dir (io/canonical-path (io/as-file examples-dir "failures")))
(def focus-dir (io/canonical-path (io/as-file examples-dir "focus")))

(declare runner reporters)

(describe "StandardRunner"
  (with runner (sut/new-standard-runner))
  (with reporters [(silent/new-silent-reporter)])

  (it "returns 0 failures when all tests pass"
    (should= 0 (running/run-directories @runner [prime-factors-dir] @reporters)))

  (it "returns lots-o failures when running failure example"
    (should= 8 (running/run-directories @runner [failures-dir] @reporters)))

  (it "limits execution to focused components"
    (running/run-directories @runner [focus-dir] @reporters)
    (should= ["yes-2" "yes-4" "yes-5" "yes-6"]
             (->> @(.-results @runner)
                  (map #(.-characteristic %))
                  (map #(.-name %)))))

  (it "config with defaults"
    (let [defaults (dissoc config/default-config :runner)]
      (should= defaults (sut/config-with-defaults []))
      (should= (assoc defaults :foo :bar) (sut/config-with-defaults [:foo :bar]))
      (should= (assoc defaults :foo :bar :baz "buzz") (sut/config-with-defaults [:foo :bar "baz" "buzz"]))))

  (context "freshening"
    (with-stubs)
    (redefs-around [fresh/load-clj-files-in (stub :load-clj-files-in)])

    (it "only freshens spec dirs, not source dirs"
      (binding [config/*specs* ["spec-dir"]
                config/*sources* ["src-dir"]]
        (running/run-directories @runner ["src-dir" "spec-dir"] @reporters)
        (should-have-invoked :load-clj-files-in {:with [["spec-dir"]]})))
    )

  (spec-helper/test-get-descriptions sut/new-standard-runner)
  (spec-helper/test-description-filtering sut/new-standard-runner)

  (context "line-filter integration"

    (defn- char-names [runner]
      (->> @(.-results runner)
           (map #(.-name (.-characteristic %)))
           sort))

    (it "runs only the chosen characteristic when a file:line target matches"
      (let [r         (sut/new-standard-runner)
            d         (components/new-description "top" false "speclj.run.standard-spec" {:file "foo.clj" :line 3})
            c1        (components/new-characteristic "first"  nil (fn [] :ok) false {:file "foo.clj" :line 10})
            c2        (components/new-characteristic "second" nil (fn [] :ok) false {:file "foo.clj" :line 20})]
        (components/install c1 d)
        (components/install c2 d)
        (running/submit-description r d)
        (binding [config/*line-targets* {"foo.clj" 20}]
          (running/run-and-report r [(silent/new-silent-reporter)]))
        (should= ["second"] (char-names r))))

    (it "line-filter wins over focus-it when both are present"
      (let [r  (sut/new-standard-runner)
            d  (components/new-description "top" false "speclj.run.standard-spec" {:file "foo.clj" :line 3})
            c1 (components/new-characteristic "target"  nil (fn [] :ok) false {:file "foo.clj" :line 10})
            c2 (components/new-characteristic "focused" nil (fn [] :ok) true  {:file "foo.clj" :line 20})]
        (components/install c1 d)
        (components/install c2 d)
        (running/submit-description r d)
        (binding [config/*line-targets* {"foo.clj" 10}]
          (running/run-and-report r [(silent/new-silent-reporter)]))
        (should= ["target"] (char-names r))))

    (it "warns via *run-unmatched* when a file:line target doesn't match but still runs untargeted files"
      (let [r         (sut/new-standard-runner)
            d         (components/new-description "top" false "speclj.run.standard-spec" {:file "foo.clj" :line 3})
            c         (components/new-characteristic "only" nil (fn [] :ok) false {:file "foo.clj" :line 10})
            unmatched (atom [])]
        (components/install c d)
        (running/submit-description r d)
        (binding [config/*line-targets*       {"bogus.clj" 10}
                  line-filter/*run-unmatched* unmatched]
          (running/run-and-report r [(silent/new-silent-reporter)]))
        (should= ["bogus.clj:10"] @unmatched)
        (should= ["only"] (char-names r))))

    (it "runs untargeted files fully alongside a narrowed targeted file"
      (let [r          (sut/new-standard-runner)
            d-narrowed (components/new-description "narrowed" false "speclj.run.standard-spec" {:file "foo.clj" :line 3})
            c1         (components/new-characteristic "target"    nil (fn [] :ok) false {:file "foo.clj" :line 10})
            c2         (components/new-characteristic "skip-me"   nil (fn [] :ok) false {:file "foo.clj" :line 20})
            d-other    (components/new-description "untargeted" false "speclj.run.standard-spec" {:file "bar.clj" :line 3})
            c3         (components/new-characteristic "runs-too" nil (fn [] :ok) false {:file "bar.clj" :line 15})]
        (components/install c1 d-narrowed)
        (components/install c2 d-narrowed)
        (components/install c3 d-other)
        (running/submit-description r d-narrowed)
        (running/submit-description r d-other)
        (binding [config/*line-targets* {"foo.clj" 10}]
          (running/run-and-report r [(silent/new-silent-reporter)]))
        (should= ["runs-too" "target"] (char-names r))))
    )
  )

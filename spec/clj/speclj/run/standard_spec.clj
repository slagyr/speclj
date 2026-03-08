(ns speclj.run.standard-spec
  (:require [speclj.config :as config]
            [speclj.core :refer :all]
            [speclj.freshener :as fresh]
            [speclj.io :as io]
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
  )

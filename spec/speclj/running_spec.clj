(ns speclj.running-spec
  (:require [speclj.config :refer [*reporters* *runner* *tag-filter*]]
            [speclj.core :refer [after around before-all context describe
                                 it should should-fail should-not
                                 should-not-throw should-not= should= tags
                                 with with-all]]
            [speclj.platform :as platform]
            [speclj.report.silent :refer [new-silent-reporter]]
            [speclj.results :refer [fail?]]
            [speclj.run.standard :as standard]
            [speclj.running :as sut]
            [speclj.spec-helper :as spec-helper]))

(def bauble (atom nil))

(describe "Running"
  (with runner (standard/new-standard-runner))
  (around [_]
    (binding [*runner*    @runner
              *reporters* [(new-silent-reporter)]
              *ns*        (the-ns 'speclj.running-spec)]
      (_)))

  (it "tracks one pass"
    (eval
      '(describe "Dummy"
         (it "has a pass"
           (should (= 1 1)))))
    (sut/run-and-report *runner* *reporters*)
    (let [results @(.-results *runner*)
          result  (first results)]
      (should= 1 (count results))
      (should= "has a pass" (.-name (.-characteristic result)))
      (should-not (fail? result))))

  (it "tracks one fail"
    (eval
      '(describe "Dummy"
         (it "has a fail"
           (should= 1 2))))
    (sut/run-and-report *runner* *reporters*)
    (let [results @(.-results *runner*)
          result  (first results)]
      (should= 1 (count results))
      (should= "has a fail" (.-name (.-characteristic result)))
      (should-not= nil (.-failure result))
      (should (platform/failure? (.-failure result)))))

  (it "runs afters with failures"
    (eval
      '(describe "Dummy"
         (after (reset! bauble nil))
         (it "changes the bauble"
           (reset! bauble :something)
           (should-fail))))
    (sut/run-and-report *runner* *reporters*)
    (should= nil @bauble))

  (it "runs afters with error"
    (eval
      '(describe "Dummy"
         (after (reset! bauble nil))
         (it "changes the bauble"
           (reset! bauble :something)
           (throw (Exception. "blah")))))
    (sut/run-and-report *runner* *reporters*)
    (should= nil @bauble))

  (it "doesn't crash when declaring a with named the same as a pre-existing var"
    (let [spec
          `(describe "Dummy"
             (with bauble "foo")
             (it "uses the new value of bauble"
               (should= "foo" @bauble)))]
      (should-not-throw (eval spec))
      (sut/run-and-report *runner* *reporters*)
      (let [results @(.-results *runner*)
            result  (first results)]
        (should= 1 (count results))
        (should-not (fail? result)))))

  (it "only executes contexts that pass the tag filter"
    (eval
      `(describe "Dummy" (tags :one)
                         (it "one tag" :filler)
                         (context "Fool" (tags :two)
                           (it "one, two tag" :filler))))
    (binding [*tag-filter* {:includes #{:one :two} :excludes #{}}]
      (sut/run-and-report *runner* *reporters*))
    (let [results @(.-results *runner*)]
      (should= 1 (count results))
      (should= "one, two tag" (.-name (.-characteristic (first results))))))

  (it "before-all's can use with-all's"
    (eval
      `(describe "Dummy"
         (with-all foo (atom 42))
         (before-all (swap! @foo inc))
         (it "check foo"
           (should= 43 @@foo))))
    (sut/run-and-report *runner* *reporters*)
    (let [results @(.-results *runner*)]
      (should= 1 (count results))
      (should-not (fail? (first results)))))

  (context "exporting"
    (spec-helper/test-exported-meta sut/filter-descriptions)
    )

  )

(describe "namespace"
  (it "runs in the current namespace"
    (should= 'speclj.running-spec (.name *ns*))))

;(run-specs :color true)

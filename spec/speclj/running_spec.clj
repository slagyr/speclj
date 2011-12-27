(ns speclj.running-spec
  (:use
    [speclj.core]
    [speclj.running :only (run-and-report)]
    [speclj.results :only (pass? fail?)]
    [speclj.run.standard :only (new-standard-runner)]
    [speclj.config :only (*reporters* *runner* *tag-filter*)]
    [speclj.report.silent :only (new-silent-reporter)])
  (:import (speclj SpecFailure)))

(def bauble (atom nil))

(describe "Running"
  (with runner (new-standard-runner))
  (around [_]
    (binding [*runner* @runner
              *reporters* [(new-silent-reporter)]
              *ns* (the-ns 'speclj.running-spec)]
      (_)))

  (it "tracks one pass"
    (eval
      '(describe "Dummy"
         (it "has a pass"
           (should (= 1 1)))))
    (run-and-report *runner* *reporters*)
    (let [results @(.results *runner*)
          result (first results)]
      (should= 1 (count results))
      (should= "has a pass" (.name (.characteristic result)))
      (should-not (fail? result))))

;  (it "tracks one fail"
;    (eval
;      '(describe "Dummy"
;         (it "has a fail"
;           (should= 1 2))))
;    (run-and-report *runner* *reporters*)
;    (let [results @(.results *runner*)
;          result (first results)]
;      (should= 1 (count results))
;      (should= "has a fail" (.name (.characteristic result)))
;      (should-not= nil (.failure result))
;      (should= SpecFailure (class (.failure result)))))
;
;  (it "runs afters with failures"
;    (eval
;      '(describe "Dummy"
;         (after (reset! bauble nil))
;         (it "changes the bauble"
;           (reset! bauble :something)
;           (should-fail))))
;    (run-and-report *runner* *reporters*)
;    (should= nil @bauble))
;
;  (it "runs afters with error"
;    (eval
;      '(describe "Dummy"
;         (after (reset! bauble nil))
;         (it "changes the bauble"
;           (reset! bauble :something)
;           (throw (Exception. "blah")))))
;    (run-and-report *runner* *reporters*)
;    (should= nil @bauble))
;
;  (it "doesn't crash when declaring a with named the same as a pre-existing var"
;    (let [spec
;          `(describe "Dummy"
;             (with bauble "foo")
;             (it "uses the new value of bauble"
;               (should= "foo" @bauble)))]
;      (should-not-throw (eval spec))
;      (run-and-report *runner* *reporters*)
;      (let [results @(.results *runner*)
;            result (first results)]
;        (should= 1 (count results))
;        (should-not (fail? result)))))
;
;  (it "only executed contexts that pass the tag filter"
;    (let [spec
;          (eval
;            `(describe "Dummy" (tags :one)
;               (it "one tag" :filler)
;               (context "Fool" (tags :two)
;                 (it "one, two tag" :filler))))]
;      (binding [*tag-filter* {:includes #{:one :two} :excludes #{}}]
;        (run-and-report *runner* *reporters*))
;      (let [results @(.results *runner*)]
;        (should= 1 (count results))
;        (should= "one, two tag" (.name (.characteristic (first results)))))))

  )

(run-specs)
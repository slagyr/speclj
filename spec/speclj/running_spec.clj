(ns speclj.running-spec
  (:use
    [speclj.core]
    [speclj.running :only (*runner* run-and-report)]
    [speclj.run.standard :only (new-standard-runner)]
    [speclj.reporting :only (*reporter*)]
    [speclj.report.silent :only (new-silent-reporter)])
  (:import (speclj SpecFailure)))

(def bauble (atom nil))

(describe "Running"
  (with runner (new-standard-runner))
  (around [_]
    (binding [*runner* @runner
              *reporter* (new-silent-reporter)
              *ns* (the-ns 'speclj.running-spec)]
      (_)))

  (it "tracks one pass"
    (eval
      '(describe "Dummy"
        (it "has a pass"
          (should (= 1 1)))))
    (run-and-report *runner* *reporter*)
    (let [results @(.results *runner*)
          result (first results)]
      (should= 1 (count results))
      (should= "has a pass" (.name (.characteristic result)))
      (should= nil (.failure result))))

  (it "tracks one fail"
    (eval
      '(describe "Dummy"
        (it "has a fail"
          (should= 1 2))))
    (run-and-report *runner* *reporter*)
    (let [results @(.results *runner*)
          result (first results)]
      (should= 1 (count results))
      (should= "has a fail" (.name (.characteristic result)))
      (should-not= nil (.failure result))
      (should= SpecFailure (class (.failure result)))))

  (it "runs afters with failures"
    (eval
      '(describe "Dummy"
        (after (reset! bauble nil))
        (it "changes the bauble"
          (reset! bauble :something)
          (should-fail))))
    (run-and-report *runner* *reporter*)
    (should= nil @bauble))

  (it "runs afters with error"
    (eval
      '(describe "Dummy"
        (after (reset! bauble nil))
        (it "changes the bauble"
          (reset! bauble :something)
          (throw (Exception. "blah")))))
    (run-and-report *runner* *reporter*)
    (should= nil @bauble))

  (it "doesn't crash when declaring a with named the same as a pre-existing var"
    (let [spec
          `(describe "Dummy"
            (with bauble "foo")
            (it "uses the new value of bauble"
              (should= "foo" @bauble)))]
      (should-not-throw (eval spec))
      (run-and-report *runner* *reporter*)
      (let [results @(.results *runner*)
            result (first results)]
        (should= 1 (count results))
        (should= nil (.failure result)))))

  )

(run-specs)
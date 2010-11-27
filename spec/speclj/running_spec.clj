(ns speclj.running-spec
  (:use
    [speclj.core]
    [speclj.running :only (*runner*)]
    [speclj.run.standard :only (new-standard-runner)]
    [speclj.reporting :only (*reporter*)]
    [speclj.report.silent :only (new-silent-reporter)])
  (:import (speclj SpecFailure)))

(describe "MultiRunner"
  (with runner (new-standard-runner))
  (around [_]
    (binding [*runner* @runner
              *reporter* (new-silent-reporter)]
      (_)))

  (it "tracks one pass"
    (describe "Dummy"
      (it "has a pass"
        (should (= 1 1))))
    (let [results @(.results *runner*)
          result (first results)]
      (should= 1 (count results))
      (should= "has a pass" (.name (.characteristic result)))
      (should= nil (.failure result))))

  (it "tracks one fail"
    (describe "Dummy"
      (it "has a fail"
        (should= 1 2)))
    (let [results @(.results *runner*)
          result (first results)]
      (should= 1 (count results))
      (should= "has a fail" (.name (.characteristic result)))
      (should-not= nil (.failure result))
      (should= SpecFailure (class (.failure result)))))

  )

(run-specs)
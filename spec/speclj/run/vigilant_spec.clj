(ns speclj.run.vigilant-spec
  (:use
    [speclj.core]
    [speclj.run.vigilant]))

(describe "Vigilant Runner"
  (with runner (new-vigilant-runner))

  (it "can be created"
    ;(should= nil @(.reloader @runner))
    (should= [] @(.results @runner)))

  )

(run-specs)
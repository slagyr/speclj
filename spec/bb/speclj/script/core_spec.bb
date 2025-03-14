(ns speclj.script.core-spec
  (:require [clojure.java.shell :as shell]
            [speclj.core :refer :all]
            [speclj.script.spec-helper :as bb-helper]
            [speclj.script.core :as sut]))

(describe "Script"
  (with-stubs)
  (bb-helper/stub-shell)
  (bb-helper/stub-system-exit)

  (context "sh"

    (it "applies shell/sh"
      (sut/sh "foo")
      (should-have-invoked :shell/sh {:with ["foo"]})
      (sut/sh "bar" "foo" "baz")
      (should-have-invoked :shell/sh {:with ["bar" "foo" "baz"]}))

    (it "exits on non-zero status codes"
      (sut/sh "foo")
      (should-not-have-invoked :script/system-exit)
      (with-redefs [shell/sh (constantly {:exit 1})]
        (sut/sh "foo")
        (should-have-invoked :script/system-exit {:with [1]}))
      (with-redefs [shell/sh (constantly {:exit 4})]
        (sut/sh "bar")
        (should-have-invoked :script/system-exit {:with [4]})))

    (it "prints :out"
      (with-redefs [println  (stub :println)
                    shell/sh (constantly {:exit 0 :out "the output"})]
        (sut/sh "blah")
        (should-have-invoked :println {:with ["the output"]})))

    (it "prints :err"
      (with-redefs [println  (stub :println)
                    shell/sh (constantly {:exit 0 :err "the error"})]
        (sut/sh "blah")
        (should-have-invoked :println {:with ["the error"]})))
    )
  )
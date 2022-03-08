(ns speclj.run.standard-spec
  (:require [speclj.core :refer [describe it should= with]]
            [speclj.report.silent :refer [new-silent-reporter]]
            [speclj.run.standard :refer :all]
            [speclj.running :refer [run-directories]])
  (:import (java.io File)))

(defn find-dir
  ([name] (find-dir (File. (.getCanonicalPath (File. ""))) name))
  ([file name]
   (let [examples (File. file name)]
     (if (.exists examples)
       examples
       (find-dir (.getParentFile file) name)))))

(def examples-dir (find-dir "examples"))
(def prime-factors-dir (.getCanonicalPath (File. examples-dir "prime_factors")))
(def failures-dir (.getCanonicalPath (File. examples-dir "failures")))
(def focus-it-dir (.getCanonicalPath (File. examples-dir "focus-it")))

(describe "StandardRunner"
  (with runner (new-standard-runner))
  (with reporters [(new-silent-reporter)])

  (it "returns 0 failures when all tests pass"
    (should= 0 (run-directories @runner [prime-factors-dir] @reporters)))

  (it "returns lots-o failures when running failure example"
    (should= 8 (run-directories @runner [failures-dir] @reporters)))

  (it "limits execution to focused characteristics (focus-it)"
    (should= 3 (run-directories @runner [focus-it-dir] @reporters)))

  )

(run-specs)
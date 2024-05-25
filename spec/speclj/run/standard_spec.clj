(ns speclj.run.standard-spec
  (:require [speclj.config :as config]
            [speclj.core :refer :all]
            [speclj.report.silent :as silent]
            [speclj.run.standard :as sut]
            [speclj.running :as running])
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
(def focus-dir (.getCanonicalPath (File. examples-dir "focus")))

(describe "StandardRunner"
  (with runner (sut/new-standard-runner))
  (with reporters [(silent/new-silent-reporter)])

  (it "returns 0 failures when all tests pass"
    (should= 0 (running/run-directories @runner [prime-factors-dir] @reporters)))

  (it "returns lots-o failures when running failure example"
    (should= 8 (running/run-directories @runner [failures-dir] @reporters)))

  (it "limits execution to focused components"
    (running/run-directories @runner [focus-dir] @reporters)
    (should= ["yes-1" "yes-2" "yes-3" "yes-4" "yes-5" "yes-6"]
             (->> @(.-results @runner)
                  (map #(.-characteristic %))
                  (map #(.-name %)))))

  (it "config with defaults"
    (let [defaults (dissoc config/default-config :runner)]
      (should= defaults (sut/config-with-defaults []))
      (should= (assoc defaults :foo :bar) (sut/config-with-defaults [:foo :bar]))
      (should= (assoc defaults :foo :bar :baz "buzz") (sut/config-with-defaults [:foo :bar "baz" "buzz"]))))
  )

(sut/run-specs)

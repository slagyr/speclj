(ns speclj.tags-spec
  (#?(:clj :require :cljs :require-macros)
   [speclj.core :refer [around context describe it should= tags]])
  (:require [clojure.string :as str]
            [speclj.config :refer [*runner* *reporters*]]
            [speclj.report.silent :refer [new-silent-reporter]]
            [speclj.run.standard :refer [new-standard-runner run-specs]]
            [speclj.tags :refer [tag-sets-for pass-includes? pass-excludes? pass-tag-filter? describe-filter]]))

(describe "Tags"

  (it "filters included tags"
    (should= true (pass-includes? #{} [:one]))
    (should= true (pass-includes? #{} [:two]))
    (should= true (pass-includes? #{:one} [:one]))
    (should= false (pass-includes? #{:one} [:two]))
    (should= true (pass-includes? #{:one :two} [:one :two]))
    (should= false (pass-includes? #{:one :two} [:one]))
    (should= true (pass-includes? #{:one :two} [:one :two :three]))
    (should= true (pass-includes? #{} [:one :two :three])))

  (it "filters excluded tags"
    (should= true (pass-excludes? #{} [:one]))
    (should= true (pass-excludes? #{} [:two]))
    (should= false (pass-excludes? #{:one} [:one]))
    (should= true (pass-excludes? #{:one} [:two]))
    (should= false (pass-excludes? #{:one :two} [:one :two]))
    (should= true (pass-excludes? #{:one :two} [:three :four]))
    (should= false (pass-excludes? #{:one :two} [:three :four :one])))

  (it "filters tags"
    (should= true (pass-tag-filter? {:includes #{} :excludes #{}} [:one :two :three]))
    (should= true (pass-tag-filter? {:includes #{:one} :excludes #{}} [:one :two :three]))
    (should= true (pass-tag-filter? {:includes #{:one :two :three} :excludes #{}} [:one :two :three]))
    (should= true (pass-tag-filter? {:includes #{} :excludes #{:four}} [:one :two :three]))
    (should= false (pass-tag-filter? {:includes #{} :excludes #{:one}} [:one :two :three])))

  (it "describes the filter"
    (should= nil (describe-filter {:includes #{} :excludes #{}}))
    (should= "Filtering tags. Including: one." (str/trim (describe-filter {:includes #{:one} :excludes #{}})))
    (should= "Filtering tags. Excluding: one." (str/trim (describe-filter {:includes #{} :excludes #{:one}})))
    (should= "Filtering tags. Including: one, two." (str/trim (describe-filter {:includes #{:one :two} :excludes #{}})))
    (should= "Filtering tags. Including: one. Excluding: two." (str/trim (describe-filter {:includes #{:one} :excludes #{:two}}))))

  #?(:clj
     (context "with fake runner/reporter"
       (around [_]
         (binding [*runner*    (new-standard-runner)
                   *reporters* (new-silent-reporter)
                   *ns*        (the-ns 'speclj.tags-spec)]
           (_)))

       (it "finds all the tag sets with one context"
         (let [spec (eval '(describe "foo"))]
           (should= [#{}] (tag-sets-for spec)))
         (let [spec (eval '(describe "foo" (tags :one)))]
           (should= [#{:one}] (tag-sets-for spec))))

       (it "finds all the tag sets with nested contexts"
         (let [spec
                        (eval '(describe "foo" (tags :one)
                                               (context "child" (tags :two)
                                                                (context "grandchild" (tags :three :four))
                                                                (context "grandchild2" (tags :five)))
                                               (context "child2" (tags :six))))
               tag-sets (tag-sets-for spec)]
           (should= 5 (count tag-sets))
           (should= #{:one} (nth tag-sets 0))
           (should= #{:one :two} (nth tag-sets 1))
           (should= #{:one :two :three :four} (nth tag-sets 2))
           (should= #{:one :two :five} (nth tag-sets 3))
           (should= #{:one :six} (nth tag-sets 4))))

       ))
  )

(run-specs)

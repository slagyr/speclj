(ns speclj.tags-spec
  (:require [speclj.core #?(:cljs :refer-macros :default :refer) [around context describe it should= tags]]
            [clojure.string :as str]
            [speclj.config :as config]
            [speclj.report.silent :as silent]
            [speclj.run.standard :as standard]
            [speclj.tags :as sut]))

(describe "Tags"

  (it "filters included tags"
    (should= true (sut/pass-includes? #{} [:one]))
    (should= true (sut/pass-includes? #{} [:two]))
    (should= true (sut/pass-includes? #{:one} [:one]))
    (should= false (sut/pass-includes? #{:one} [:two]))
    (should= true (sut/pass-includes? #{:one :two} [:one :two]))
    (should= false (sut/pass-includes? #{:one :two} [:one]))
    (should= true (sut/pass-includes? #{:one :two} [:one :two :three]))
    (should= true (sut/pass-includes? #{} [:one :two :three])))

  (it "filters excluded tags"
    (should= true (sut/pass-excludes? #{} [:one]))
    (should= true (sut/pass-excludes? #{} [:two]))
    (should= false (sut/pass-excludes? #{:one} [:one]))
    (should= true (sut/pass-excludes? #{:one} [:two]))
    (should= false (sut/pass-excludes? #{:one :two} [:one :two]))
    (should= true (sut/pass-excludes? #{:one :two} [:three :four]))
    (should= false (sut/pass-excludes? #{:one :two} [:three :four :one])))

  (it "filters tags"
    (should= true (sut/pass-tag-filter? {:includes #{} :excludes #{}} [:one :two :three]))
    (should= true (sut/pass-tag-filter? {:includes #{:one} :excludes #{}} [:one :two :three]))
    (should= true (sut/pass-tag-filter? {:includes #{:one :two :three} :excludes #{}} [:one :two :three]))
    (should= true (sut/pass-tag-filter? {:includes #{} :excludes #{:four}} [:one :two :three]))
    (should= false (sut/pass-tag-filter? {:includes #{} :excludes #{:one}} [:one :two :three])))

  (it "describes the filter"
    (should= nil (sut/describe-filter {:includes #{} :excludes #{}}))
    (should= "Filtering tags. Including: one." (str/trim (sut/describe-filter {:includes #{:one} :excludes #{}})))
    (should= "Filtering tags. Excluding: one." (str/trim (sut/describe-filter {:includes #{} :excludes #{:one}})))
    (should= "Filtering tags. Including: one, two." (str/trim (sut/describe-filter {:includes #{:one :two} :excludes #{}})))
    (should= "Filtering tags. Including: one. Excluding: two." (str/trim (sut/describe-filter {:includes #{:one} :excludes #{:two}}))))

  #?(:cljs (list)
     :default
     (context "with fake runner/reporter"
       (around [_]
         (binding [config/*runner*    (standard/new-standard-runner)
                   config/*reporters* (silent/new-silent-reporter)
                   *ns*               (the-ns 'speclj.tags-spec)]
           (_)))

       (it "finds all the tag sets with one context"
         (let [spec (eval '(describe "foo"))]
           (should= [#{}] (sut/tag-sets-for spec)))
         (let [spec (eval '(describe "foo" (tags :one)))]
           (should= [#{:one}] (sut/tag-sets-for spec))))

       (it "finds all the tag sets with nested contexts"
         (let [spec
                        (eval '(describe "foo" (tags :one)
                                               (context "child" (tags :two)
                                                 (context "grandchild" (tags :three :four))
                                                 (context "grandchild2" (tags :five)))
                                               (context "child2" (tags :six))))
               tag-sets (sut/tag-sets-for spec)]
           (should= 5 (count tag-sets))
           (should= #{:one} (nth tag-sets 0))
           (should= #{:one :two} (nth tag-sets 1))
           (should= #{:one :two :three :four} (nth tag-sets 2))
           (should= #{:one :two :five} (nth tag-sets 3))
           (should= #{:one :six} (nth tag-sets 4))))

       ))
  )

(standard/run-specs)

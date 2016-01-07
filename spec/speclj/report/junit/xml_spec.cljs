(ns speclj.report.junit.xml-spec
  (:require-macros [speclj.core :refer [describe it should= should-not-throw]])
  (:require [speclj.core]
            [speclj.report.junit.xml :as xml]))

(describe "ClojureScript XML"
  (it "generates XML with semantics similar to clojure.data.xml"
    (let [testcase (xml/element :testcase
                    {:classname "A test works?"
                     :name "A test works?"
                     :time "0.0001"})
          testsuite (xml/element :testsuite
                     {:name "speclj"
                      :errors 0
                      :skipped 0
                      :tests 1
                      :failures 0
                      :time "0.0001"
                      :timestamp (js/Date. "2015-12-14T21:04:48.631Z")}
                     testcase)
          testsuites (xml/element :testsuites {} testsuite)]
      (should= (xml/emit-str testsuites)
               (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    "<testsuites>"
                      "<testsuite name=\"speclj\" errors=\"0\" skipped=\"0\" "
                        "tests=\"1\" failures=\"0\" time=\"0.0001\" "
                        "timestamp=\"2015-12-14T21:04:48.631Z\">"
                        "<testcase classname=\"A test works?\" "
                          "name=\"A test works?\" time=\"0.0001\"></testcase>"
                      "</testsuite>"
                    "</testsuites>")))))

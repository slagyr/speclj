(ns speclj.report.junit-spec
  (#?(:clj :require :cljs :require-macros)
    [speclj.core :refer [around before context describe it should should=
                         with -new-exception -new-failure -new-pending]])
  (:require [clojure.string :refer [join split-lines]]
    #?(:clj  [clj-time.core :as t]
       :cljs [cljs-time.core :as t])
    #?(:cljs [goog.string])                                 ;cljs bug?
            [speclj.components :refer [new-description new-characteristic install]]
            [speclj.config :refer [*color?*]]
            [speclj.platform :refer [endl format-seconds]]
            [speclj.report.junit :refer [now new-junit-reporter]]
            [speclj.reporting :refer [report-runs]]
            [speclj.results :refer [pass-result fail-result pending-result error-result]]
            [speclj.run.standard :refer [run-specs]]))

(def date-time (t/date-time 1970 1 1))
(def date #?(:clj  date-time
             :cljs (.-date date-time)))
(def date-str "1970-01-01T00:00:00.000Z")

(def default-counts
  {:errors 0 :skipped 0 :tests 1 :failures 0})

(defn xml [counts & strs]
  (let [{:keys [errors skipped tests failures]} (merge default-counts counts)]
    (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          "<testsuites>"
            "<testsuite name=\"speclj\" "
              "errors=\"" errors "\" skipped=\"" skipped "\" "
              "tests=\"" tests "\" failures=\"" failures "\" "
              "time=\"0\" timestamp=\"" date-str "\">"
              (apply str strs)
            "</testsuite>"
          "</testsuites>"
          endl)))

(defn testcase [class-name & strs]
  (str "<testcase classname=\"" class-name "\" name=\"" class-name "\" time=\"0\">"
       (apply str strs)
       "</testcase>"))

(defn exception [message]
  #?(:clj (Exception. message)
     :cljs (Error. message)))

(defn class-name [x]
  #?(:clj  (.. x getClass getCanonicalName)
     :cljs (.-name (type x))))

(defn stack-trace-str [e]
  #?(:clj  (join "\n" (.getStackTrace e))
     :cljs (str (.-stack e))))

(describe "JUnit Reporter"
  (with reporter (new-junit-reporter))
  (with description (new-description "JUnit" "some-ns"))

  (around [it]
    (with-redefs [format-seconds (fn [arg] "0")
                  t/now (fn [] date)]
      (it)))

  (it "reports errors"
    (let [e (ex-info "welp" {})
          result (error-result e)
          reporter @reporter]
      (should= (xml {:errors 1 :tests 0}
                    (testcase (class-name e)
                              "<error message=\"welp\">"
                              (stack-trace-str e)
                              "</error>"))
               (with-out-str (report-runs reporter [result])))))

  (it "reports pass"
    (let [characteristic (new-characteristic "says pass" @description "pass")
          result (pass-result characteristic 1)]
      (should= (xml default-counts
                    (testcase "JUnit says pass"))
               (with-out-str (report-runs @reporter [result])))))

  (it "reports pending"
    (let [characteristic (new-characteristic "pending!" `(pending))
          result (pending-result characteristic 1 (-new-pending "some reason for pendiness"))]
      (should= (xml {:skipped 1}
                    (testcase "pending!"
                              "<skipped>"
                                "some reason for pendiness"
                              "</skipped>"))
               (with-out-str (report-runs @reporter [result])))))

  (it "reports fail"
    (let [characteristic (new-characteristic "says fail" @description "fail")
          result (fail-result characteristic 2 (-new-failure "blah"))]
      (should= (xml {:failures 1}
                    (testcase "JUnit says fail"
                              "<failure message=\"test failure\">"
                                "blah"
                              "</failure>"))
               (with-out-str (report-runs @reporter [result])))))

  (context "with nested description"
    (with nested-description (new-description "Nesting" "some.ns"))
    (before (install @nested-description @description))

    (it "reports nested pass"
      (let [characteristic (new-characteristic "nested pass" @nested-description "pass")
            result (pass-result characteristic 1)]
        (should= (xml default-counts
                    (testcase "JUnit Nesting nested pass"))
                 (with-out-str (report-runs @reporter [result])))))

    (it "reports nested failure"
      (let [characteristic (new-characteristic "nested fail" @nested-description "fail")
            result (fail-result characteristic 2 (-new-failure "blah"))]
        (should= (xml {:failures 1}
                    (testcase "JUnit Nesting nested fail"
                              "<failure message=\"test failure\">"
                                "blah"
                              "</failure>"))
                 (with-out-str (report-runs @reporter [result])))))))

(run-specs)

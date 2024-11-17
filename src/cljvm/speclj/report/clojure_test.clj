(ns speclj.report.clojure-test
  (:require [clojure.string]
            [clojure.string :as string :refer [split]]
            [clojure.test]
            [speclj.platform :as platform]
            [speclj.io :as io]
            [speclj.reporting]))

(defn full-name [characteristic]
  (loop [context @(.-parent characteristic) name (.-name characteristic)]
    (if context
      (recur @(.-parent context) (str (.-name context) " " name))
      name)))

(defn- classname->filename [classname]
  (let [root-name (first (split classname #"\$"))]
    (str
      (string/replace root-name "." platform/file-separator)
      ".clj")))

(defn failure-source [failure]
  (let [source    (nth (.getStackTrace failure) 0)
        classname (.getClassName source)
        filename  (classname->filename classname)]
    (if-let [url (.getResource (clojure.lang.RT/baseLoader) filename)]
      {:file (io/as-file url) :line (.getLineNumber source)}
      {:file filename :line (.getLineNumber source)})))

(deftype ClojureTestReporter [report-counters]
  speclj.reporting/Reporter
  (report-message [_this message]
    (println message) (flush))

  (report-description [_this _description])

  (report-pass [_this _result]
    (binding [clojure.test/*report-counters* report-counters]
      (clojure.test/inc-report-counter :test)
      (clojure.test/report {:type :pass})))

  (report-pending [_this _result]
    (binding [clojure.test/*report-counters* report-counters]
      (clojure.test/inc-report-counter :test)
      (clojure.test/inc-report-counter :pending)))

  (report-fail [_this result]
    (binding [clojure.test/*report-counters* report-counters]
      (clojure.test/inc-report-counter :test)
      (let [characteristic      (.-characteristic result)
            failure             (.-failure result)
            characteristic-text (full-name characteristic)]
        (binding [clojure.test/*testing-vars* [(with-meta {} {:name (symbol characteristic-text)})]]
          (clojure.test/report
            (merge
              {:type :fail}
              (failure-source failure)
              {:message  (platform/error-message failure)
               :expected "see above"
               :actual   "see above"}))))))

  (report-error [_this result]
    (binding [clojure.test/*report-counters* report-counters]
      (let [ex (.-exception result)]
        (binding [clojure.test/*testing-vars* [(with-meta {} {:name (symbol "unknown")})]]
          (clojure.test/report
            (merge
              {:type :error}
              (failure-source ex)
              {:message  (.getMessage ex)
               :expected "not recorded"
               :actual   ex}))))))

  (report-runs [_this _results]
    (binding [clojure.test/*report-counters* report-counters]
      (clojure.test/report (merge {:type :summary} @report-counters)))))

(defn new-clojure-test-reporter []
  (ClojureTestReporter. (ref clojure.test/*initial-report-counters*)))

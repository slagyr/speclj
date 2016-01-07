(ns speclj.report.junit
  (:require [clojure.string :as string]
            [speclj.reporting]
            [speclj.platform :refer [format-seconds]]
            [speclj.report.progress :refer [full-name]]
            [speclj.reporting :refer [tally-time]]
            [speclj.results]
            [clojure.string :refer [trim-newline]]
#?(:clj     [clojure.data.xml :as xml]
   :cljs    [speclj.report.junit.xml :as xml])
#?(:clj     [clj-time.core :as t]
   :cljs    [cljs-time.core :as t])
#?(:cljs    [speclj.results :refer [PassResult FailResult PendingResult ErrorResult]]))
  #?(:clj (:import [speclj.results PassResult FailResult PendingResult ErrorResult])))

(def pass-count (atom 0))
(def error-count (atom 0))
(def fail-count (atom 0))
(def skipped-count (atom 0))

(defn- clear-atom [a]
  (swap! a (fn [_] 0)))

(defn- reset-counters []
  (clear-atom pass-count)
  (clear-atom error-count)
  (clear-atom fail-count)
  (clear-atom skipped-count))

(defn- test-count [] (+ @pass-count @fail-count @skipped-count))

(defn now []
  (t/now))

(defn failure-message [failure]
  (try
    (.getMessage failure)
    (catch #?(:clj Exception :cljs :default) e
      (.-message failure))))

(defn- pass->xml [result]
  (swap! pass-count inc)
  (let [characteristic (.-characteristic result)
        spec-name (full-name characteristic)
        seconds (format-seconds (.-seconds result))]
    (xml/element :testcase {:classname spec-name :name spec-name :time seconds})))

(defn- pending->xml [result]
  (swap! skipped-count inc)
  (let [characteristic (.-characteristic result)
        spec-name (full-name characteristic)
        seconds (format-seconds (.-seconds result))
        exception (.-exception result)]
    (xml/element :testcase {:classname spec-name :name spec-name :time seconds}
                 (xml/element :skipped {} (failure-message exception)))))

(defn- fail->xml [result]
  (swap! fail-count inc)
  (let [characteristic (.-characteristic result)
        spec-name (full-name characteristic)
        seconds (format-seconds (.-seconds result))
        failure (.-failure result)]
    (xml/element :testcase {:classname spec-name :name spec-name :time seconds}
                 (xml/element :failure {:message "test failure"} (failure-message failure)))))

(defn class-name [x]
  #?(:clj  (.. x getClass getCanonicalName)
     :cljs (.-name (type x))))

(defn stack-trace-str [e]
  #?(:clj  (string/join "\n" (.getStackTrace e))
     :cljs (str (.-stack e))))

(defn message [e]
  #?(:clj  (.getMessage e)
     :cljs (.-message e)))

(defn- error-result->xml [result]
   (swap! error-count inc)
   (let [exception (.-exception result)
         class-name (class-name exception)
         message (failure-message exception)
         stacktrace (stack-trace-str exception)
         seconds (format-seconds (.-seconds result))]
        (xml/element :testcase {:classname class-name :name class-name :time seconds}
          (xml/element :error {:message message} stacktrace))))

(defn- result->xml [result]
  (cond
   (= PassResult (type result))
   (pass->xml result)

   (= PendingResult (type result))
   (pending->xml result)

   (= FailResult (type result))
   (fail->xml result)

   (= ErrorResult (type result))
   (error-result->xml result)

   :else
   (println (str "Unknown result type: " (type result)))))

(defn- runs->xml [results]
  (reset-counters)
  (let [xml-results (doall (map result->xml results))]
    (xml/element :testsuites {}
                 (xml/element :testsuite {:name "speclj"
                                          :errors @error-count
                                          :skipped @skipped-count
                                          :tests (test-count)
                                          :failures @fail-count
                                          :time (format-seconds (tally-time results))
                                          :timestamp (now)}
                              xml-results))))

(deftype JUnitReporter [passes fails results]
  speclj.reporting/Reporter
  (report-message [this message])
  (report-description [this description])
  (report-pass [this result])
  (report-pending [this result])
  (report-fail [this result])
  (report-runs [this results]
    (println (xml/emit-str (runs->xml results))))
  (report-error [this exception]))

(defn new-junit-reporter []
  (JUnitReporter. (atom 0) (atom 0) (atom nil)))

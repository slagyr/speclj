(ns speclj.run.vigilant
  (:require [clojure.java.io :refer [file]]
            [fresh.core :refer [clj-files-in make-fresh ns-to-file]]
            [speclj.config :refer [active-reporters active-runner config-bindings]]
            [speclj.platform :refer [current-time endl enter-pressed? format-seconds secs-since]]
            [speclj.reporting :refer [report-message* report-runs*]]
            [speclj.results :refer [categorize]]
            [speclj.running :refer [do-description filter-focused process-compile-error run-and-report run-description]])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)
           (speclj.running Runner)))

(def start-time (atom 0))

(defn- ns-for-results [results]
  (set (map #(str (.ns @(.. % characteristic parent))) results)))

(defn- report-update [report]
  (let [reporters (active-reporters)
        reloads   (:reloaded report)]
    (when (seq reloads)
      (report-message* reporters (str endl "----- " (str (java.util.Date.) " -----")))
      (report-message* reporters (str "took " (format-seconds (secs-since @start-time)) " to determine file statuses."))
      (report-message* reporters "reloading files:")
      (doseq [file reloads] (report-message* reporters (str "  " (.getCanonicalPath file))))))
  true)

(defn- reload-files [runner current-results]
  (let [previous-failed-files (map ns-to-file (ns-for-results @(.previous-failed runner)))
        files-to-reload       (set (concat previous-failed-files current-results))]
    (swap! (.file-listing runner) #(apply dissoc % previous-failed-files))
    (make-fresh (.file-listing runner) files-to-reload report-update)))

(defn- reload-report [runner report]
  (let [reloads (:reloaded report)]
    (when (seq reloads)
      (reload-files runner reloads)))
  false)

(defn- tick [configuration]
  (with-bindings configuration
    (let [runner    (active-runner)
          reporters (active-reporters)]
      (try
        (reset! start-time (current-time))
        (make-fresh (.file-listing runner) (set (apply clj-files-in @(.directories runner))) (partial reload-report runner))
        (when (seq @(.descriptions runner))
          (reset! (.previous-failed runner) (:fail (categorize (seq @(.results runner)))))
          (run-and-report runner reporters))
        (catch java.lang.Throwable e
          (process-compile-error runner e)
          (report-runs* reporters @(.results runner))))
      (reset! (.descriptions runner) [])
      (reset! (.results runner) []))))

(defn- reset-runner [runner]
  (reset! (.previous-failed runner) [])
  (reset! (.results runner) [])
  (reset! (.file-listing runner) {}))

(defn- listen-for-rerun [configuration]
  (with-bindings configuration
    (when (enter-pressed?)
      (reset-runner (active-runner)))))

(deftype VigilantRunner [file-listing results previous-failed directories descriptions]
  Runner
  (run-directories [this directories reporters]
    (let [scheduler     (ScheduledThreadPoolExecutor. 1)
          configuration (config-bindings)
          runnable      (fn [] (tick configuration))
          dir-files     (map file directories)]
      (reset! (.directories this) dir-files)
      (.scheduleWithFixedDelay scheduler runnable 0 500 TimeUnit/MILLISECONDS)
      (.scheduleWithFixedDelay scheduler (fn [] (listen-for-rerun configuration)) 0 500 TimeUnit/MILLISECONDS)
      (.awaitTermination scheduler Long/MAX_VALUE TimeUnit/SECONDS)
      0))

  (submit-description [this description]
    (swap! descriptions conj description))

  (run-description [this description reporters]
    (let [run-results (do-description description reporters)]
      (swap! results into run-results)))

  (run-and-report [this reporters]
    (doseq [description (filter-focused @descriptions)]
      (run-description this description reporters))
    (report-runs* reporters @results)))

(defn new-vigilant-runner []
  (VigilantRunner. (atom {}) (atom []) (atom []) (atom nil) (atom [])))

(ns speclj.run.vigilant
  (:require [clojure.java.io :refer [file]]
            [fresh.core :refer [freshener make-fresh ns-to-file clj-files-in]]
            [speclj.config :refer [active-runner active-reporters config-bindings *specs*]]
            [speclj.platform :refer [endl secs-since format-seconds current-time]]
            [speclj.reporting :refer [report-runs* report-message* report-error*]]
            [speclj.results :refer [categorize]]
            [speclj.running :refer [do-description run-and-report run-description process-compile-error]])
  (:import [speclj.running Runner]
           [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(def start-time (atom 0))

(defn- ns-for-results [results]
  (set (map #(str (.ns @(.. % characteristic parent))) results))
  )

(defn- report-update [report]
  (let [reporters (active-reporters)
        reloads (:reloaded report)]
    (when (seq reloads)
      (report-message* reporters (str endl "----- " (str (java.util.Date.) " -------------------------------------------------------------------")))
      (report-message* reporters (str "took " (format-seconds (secs-since @start-time)) " to determine file statuses."))
      (report-message* reporters "reloading files:")
      (doseq [file reloads] (report-message* reporters (str "  " (.getCanonicalPath file))))))
  true)

(defn- reload-files [runner current-results]
  (let [previous-failed-files (map ns-to-file (ns-for-results @(.previous-failed runner)))
        files-to-reload (set (concat previous-failed-files current-results))]
    (swap! (.file-listing runner) #(apply dissoc % previous-failed-files))
    (make-fresh (.file-listing runner) files-to-reload report-update)
    )
  )

(defn- reload-report [runner report]
  (let [reloads (:reloaded report)]
    (when (seq reloads)
      (reload-files runner reloads)
      )
    )
  false)

(defn- tick [configuration]
  (with-bindings configuration
    (let [runner (active-runner)
          reporters (active-reporters)]
      (try
        (reset! start-time (current-time))
        (make-fresh (.file-listing runner) (set (apply clj-files-in @(.directories runner))) (partial reload-report runner))
        (when (seq @(.results runner))
          (reset! (.previous-failed runner) (:fail (categorize (seq @(.results runner)))))
          (run-and-report runner reporters))
        (catch java.lang.Throwable e
          (process-compile-error runner e)
          (report-runs* reporters @(.results runner))))
      (reset! (.results runner) []))))

(deftype VigilantRunner [file-listing results previous-failed directories]
  Runner
  (run-directories [this directories reporters]
    (let [scheduler (ScheduledThreadPoolExecutor. 1)
          configuration (config-bindings)
          runnable (fn [] (tick configuration))
          dir-files (map file directories)]
      (reset! (.directories this) dir-files)
      (.scheduleWithFixedDelay scheduler runnable 0 500 TimeUnit/MILLISECONDS)
      (.awaitTermination scheduler Long/MAX_VALUE TimeUnit/SECONDS)
      0))

  (submit-description [this description]
    (run-description this description (active-reporters)))

  (run-description [this description reporters]
    (let [run-results (do-description description reporters)]
      (swap! results into run-results)))

  (run-and-report [this reporters]
    (report-runs* reporters @results))

;  Object
;  (toString [this] (str this))
  )

(defn new-vigilant-runner []
  (VigilantRunner. (atom {}) (atom []) (atom []) (atom nil)))

(ns speclj.run.vigilant
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as repl]
            [speclj.freshener :refer [make-fresh ns-to-file freshen]]
            [speclj.config :as config]
            [speclj.platform :refer [current-time endl enter-pressed? format-seconds secs-since]]
            [speclj.reporting :as reporting]
            [speclj.results :as results]
            [speclj.running :as running]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.reload :as reload])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor TimeUnit)))

(def start-time (atom 0))

(def current-error-data (atom nil))

(defn get-error-data [e]
  (:data (first (:via (Throwable->map e)))))

(defn- tick [configuration]
  (with-bindings configuration
    (let [runner (config/active-runner)
          reporters (config/active-reporters)]
      (try
        (reset! start-time (current-time))
        (freshen (.file-listing runner) @(.directories runner))
        (cond
          (::reload/error repl/refresh-tracker)
          (throw (::reload/error repl/refresh-tracker))
          (seq @(.descriptions runner))
          (do
            (reset! current-error-data nil)
            (reset! (.previous-failed runner) (:fail (results/categorize (seq @(.results runner)))))
            (running/run-and-report runner reporters)))
        (catch java.lang.Throwable e
          (let [error-data (get-error-data e)]
            (when (not= error-data @current-error-data)
              (running/process-compile-error runner e)
              (reporting/report-runs* reporters @(.results runner)))
            (reset! current-error-data error-data))
          ))
      (reset! (.descriptions runner) [])
      (reset! (.results runner) []))))

(defn- reset-runner [runner]
  (reset! (.previous-failed runner) [])
  (reset! (.results runner) [])
  (reset! (.file-listing runner) {}))

(defn- listen-for-rerun [configuration]
  (with-bindings configuration
    (when (enter-pressed?)
      (reset-runner (config/active-runner)))))

(deftype VigilantRunner [file-listing results previous-failed directories descriptions]
  running/Runner
  (run-directories [this directories _reporters]
    (let [scheduler (ScheduledThreadPoolExecutor. 1)
          configuration (config/config-bindings)
          runnable (fn [] (tick configuration))
          dir-files (map io/file directories)]
      (reset! (.directories this) dir-files)
      (apply repl/set-refresh-dirs dir-files)
      (.scheduleWithFixedDelay scheduler runnable 0 500 TimeUnit/MILLISECONDS)
      (.scheduleWithFixedDelay scheduler (fn [] (listen-for-rerun configuration)) 0 500 TimeUnit/MILLISECONDS)
      (.awaitTermination scheduler Long/MAX_VALUE TimeUnit/SECONDS)
      0))

  (submit-description [_this description]
    (swap! descriptions conj description))

  (-get-descriptions [_this] @descriptions)

  (-filter-descriptions [_this namespaces]
    (swap! descriptions running/descriptions-with-namespaces namespaces))

  (run-description [_this description reporters]
    (->> (running/do-description description reporters)
         (swap! results into)))

  (run-and-report [this reporters]
    (doseq [description (running/filter-focused @descriptions)]
      (running/run-description this description reporters))
    (reporting/report-runs* reporters @results)))

(defn new-vigilant-runner []
  (VigilantRunner. (atom {}) (atom []) (atom []) (atom nil) (atom [])))

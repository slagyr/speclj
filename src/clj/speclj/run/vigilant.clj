(ns speclj.run.vigilant
  (:require [clojure.tools.namespace.reload :as reload]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.track :as track]
            [speclj.config :as config]
            [speclj.event :as event]
            [speclj.freshener :as freshener]
            [speclj.interval :as interval]
            [speclj.io :as io]
            [speclj.platform :as platform]
            [speclj.reporting :as reporting]
            [speclj.running :as running]
            [speclj.thread :as thread]))

(defn- report-update [reporters files refresh-time]
  (when (seq files)
    (reporting/report-message* reporters (str platform/endl "----- " (platform/current-date) " -----"))
    (reporting/report-message* reporters (str "took " (platform/format-seconds refresh-time) " seconds to refresh files."))
    (reporting/report-message* reporters "reloading files:")
    (doseq [file files]
      (reporting/report-message* reporters (str "  " (io/canonical-path file))))))

(defn- report-error [e runner reporters]
  (alter-var-root #'repl/refresh-tracker
                  (constantly (assoc repl/refresh-tracker ::track/load [])))
  (running/process-compile-error runner e)
  (reporting/report-runs* reporters @(.-results runner)))

(defn- run-reloaded-files [runner reporters reloaded-files refresh-time]
  (when-let [error (::reload/error repl/refresh-tracker)]
    (throw error))
  (when (seq @(.-descriptions runner))
    (report-update reporters reloaded-files refresh-time)
    (running/run-and-report runner reporters)))

(defn- reload-files []
  (let [start          (platform/current-time)
        reloaded-files (freshener/freshen)
        elapsed        (platform/secs-since start)]
    [reloaded-files elapsed]))

(defn- tick [runner reporters configuration]
  (with-bindings configuration
    (let [[reloaded-files refresh-time] (reload-files)]
      (platform/try-catch-anything
        (run-reloaded-files runner reporters reloaded-files refresh-time)
        (catch e (report-error e runner reporters)))
      (reset! (.-descriptions runner) [])
      (reset! (.-results runner) []))))

(defn- reset-runner [runner configuration dir-files]
  (with-bindings configuration
    (repl/clear)
    (apply repl/set-refresh-dirs dir-files)
    (reset! (.-results runner) [])))

(defn- -run-directories [this directories reporters]
  (let [configuration (config/config-bindings)
        dir-files     (map io/as-file directories)]
    (apply repl/set-refresh-dirs dir-files)
    (interval/set-interval 500 #(tick this reporters configuration))
    (event/add-enter-listener #(reset-runner this configuration dir-files))
    (thread/join-all)
    0))

(deftype VigilantRunner [results descriptions]
  running/Runner
  (run-directories [this dirs reporters]
    (-run-directories this dirs reporters))

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
  (VigilantRunner. (atom []) (atom [])))

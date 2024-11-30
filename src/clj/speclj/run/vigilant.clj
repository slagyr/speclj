(ns speclj.run.vigilant
  (:require [clojure.tools.namespace.reload :as reload]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.track :as track]
            [speclj.config :as config]
            [speclj.freshener :as freshener]
            [speclj.io :as io]
            [speclj.platform :as platform]
            [speclj.reporting :as reporting]
            [speclj.results :as results]
            [speclj.running :as running]))

(def start-time (atom 0))
(def current-error-data (atom nil))

(defn get-error-data [e]
  (-> e Throwable->map :via first :data))

(defn- report-update [files start-time]
  (when (seq files)
    (let [reporters (config/active-reporters)]
      (reporting/report-message* reporters (str platform/endl "----- " (str (platform/current-date) " -----")))
      (reporting/report-message* reporters (str "took " (platform/format-seconds (platform/secs-since start-time)) " to determine file statuses."))
      (reporting/report-message* reporters "reloading files:")
      (doseq [file files]
        (reporting/report-message* reporters (str "  " (io/canonical-path file))))))
  true)

(defn- report-error [e runner reporters]
  (let [error-data (get-error-data e)]
    (alter-var-root #'repl/refresh-tracker
                    (constantly (assoc repl/refresh-tracker ::track/load [])))
    (running/process-compile-error runner e)
    (reporting/report-runs* reporters @(.results runner))
    (reset! current-error-data error-data)))

(defn- run-reloaded-files [runner reporters reloaded-files]
  (reset! start-time (platform/current-time))
  (when-let [error (::reload/error repl/refresh-tracker)]
    (throw error))
  (when (seq @(.descriptions runner))
    (report-update reloaded-files @start-time)
    (reset! current-error-data nil)
    (reset! (.previous-failed runner) (:fail (results/categorize (seq @(.results runner)))))
    (running/run-and-report runner reporters)))

(defn- tick [configuration]
  (with-bindings configuration
    (let [runner         (config/active-runner)
          reporters      (config/active-reporters)
          reloaded-files (freshener/freshen)]
      (platform/try-catch-anything
        (run-reloaded-files runner reporters reloaded-files)
        (catch e (report-error e runner reporters)))
      (reset! (.descriptions runner) [])
      (reset! (.results runner) []))))

(defn- reset-runner [runner]
  (reset! current-error-data nil)
  (repl/clear)
  (apply repl/set-refresh-dirs @(.directories runner))
  (reset! (.previous-failed runner) [])
  (reset! (.results runner) [])
  (reset! (.file-listing runner) {}))

(defn- listen-for-rerun [configuration]
  (with-bindings configuration
    (when (platform/enter-pressed?)
      (reset-runner (config/active-runner)))))

(defn- -run-directories [this directories]
  (let [configuration (config/config-bindings)
        dir-files     (map io/as-file directories)]
    (reset! (.directories this) dir-files)
    (apply repl/set-refresh-dirs dir-files)
    ;; TODO [BAC]: Instead of schedule-tasks, do this.
    ;;  JavaScript does not know about threads/tasks,
    ;;  but it does know about intervals and events
    (comment
      (platform/set-interval 500 #(tick configuration))
      (platform/on-enter-pressed #(rerun configuration))
      (platform/wait-indefinitely))
    (platform/schedule-tasks
      [{:command #(tick configuration) :delay-ms 500}
       {:command #(listen-for-rerun configuration) :delay-ms 500}])
    0))

(deftype VigilantRunner [file-listing results previous-failed directories descriptions]
  running/Runner
  (run-directories [this dirs _reporters]
    (-run-directories this dirs))

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

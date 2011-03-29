(ns speclj.run.vigilant
  (:use
    [speclj.running :only (do-description run-and-report run-description clj-files-in)]
    [speclj.util]
    [speclj.reporting :only (report-runs report-message print-stack-trace)]
    [speclj.config :only (active-runner active-reporter config-bindings *specs*)]
    [fresh.core :only (freshener)])
  (:import
    [speclj.running Runner]
    [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(def start-time (atom 0))

(defn- report-update [report]
  (let [reporter (active-reporter)
        reloads (:reloaded report)]
    (when (seq reloads)
      (report-message reporter (str endl "----- " (str (java.util.Date.) " -------------------------------------------------------------------")))
      (report-message reporter (str "took " (str-time-since @start-time) " to determine file statuses."))
      (report-message reporter "reloading files:")
      (doseq [file reloads] (report-message reporter (str "  " (.getCanonicalPath file))))))
  true)

(defn- tick [configuration]
  (with-bindings configuration
    (let [runner (active-runner)
          reporter (active-reporter)]
      (try
        (reset! start-time (System/nanoTime))
        (@(.reloader runner))
        (when (seq @(.results runner))
          (run-and-report runner reporter))
        (catch Exception e (print-stack-trace e *out*)))
      (reset! (.results runner) []))))

(deftype VigilantRunner [reloader results]
  Runner
  (run-directories [this directories reporter]
    (let [scheduler (ScheduledThreadPoolExecutor. 1)
          configuration (config-bindings)
          runnable (fn [] (tick configuration))]
      (reset! reloader (freshener #(set (apply clj-files-in directories)) report-update))
      (.scheduleWithFixedDelay scheduler runnable 0 500 TimeUnit/MILLISECONDS)
      (.awaitTermination scheduler Long/MAX_VALUE TimeUnit/SECONDS)
      0))

  (submit-description [this description]
    (run-description this description (active-reporter)))

  (run-description [this description reporter]
    (let [run-results (do-description description reporter)]
      (swap! results into run-results)))

  (run-and-report [this reporter]
    (report-runs reporter @results))

  Object
  (toString [this] (.toString this)))

(defn new-vigilant-runner []
  (VigilantRunner. (atom nil) (atom [])))


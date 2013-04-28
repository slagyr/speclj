(ns specljs.run.standard
  (:require [specljs.components]
            [specljs.config :refer [default-config default-runner-fn default-runner with-config
                                   active-reporters active-runner]]
            [specljs.report.progress] ; so that we can load the default reporter
            [specljs.reporting :refer [report-runs* report-message*]]
            [specljs.results :refer [fail-count]]
            [specljs.running :refer [do-description run-and-report run-description]]
            [specljs.tags :refer [describe-filter]]))

(def counter (atom 0))

(deftype StandardRunner [num descriptions results]
  specljs.running/Runner
  (run-directories [this directories reporters]
    (js/alert "StandardRunner.run-directories:  I don't know how to do this."))

  (submit-description [this description]
    (swap! descriptions conj description))

  (run-description [this description reporters]
    (let [run-results (do-description description reporters)]
      (swap! results into run-results)))

  (run-and-report [this reporters]
    (doseq [description @descriptions]
      (run-description this description reporters))
    (report-runs* reporters @results)))

(extend-protocol IPrintWithWriter
  specljs.run.standard.StandardRunner
  (-pr-writer [x writer opts]
    (-write writer (str "#<specljs.run.standard.StandardRunner(num: " (.-num x) ", descriptions: "))
    (-pr-writer @(.-descriptions x) writer opts)
    (-write writer ")>"))
  specljs.components.Description
  (-pr-writer [x writer opts]
    (-write writer (format "#<specljs.component.Description(name: %s)>" (.-name x)))))

(defn new-standard-runner []
  (StandardRunner. (swap! counter inc) (atom []) (atom [])))

(reset! default-runner-fn new-standard-runner)
(reset! default-runner (new-standard-runner))
(def armed false)

(defn run-specs [& configurations]
  "If evaluated outsite the context of a spec run, it will run all the specs that have been evaulated using the default
runner and reporter.  A call to this function is typically placed at the end of a spec file so that all the specs
are evaluated by evaluation the file as a script.  Optional configuration paramters may be passed in:

(run-specs :stacktrace true :color false :reporter \"documentation\")"
  (when armed
    (let [config (apply hash-map configurations)
          config (merge (dissoc default-config :runner) config)]
      (with-config config
        (fn []
          (if-let [filter-msg (describe-filter)]
            (report-message* (active-reporters) filter-msg))
          (run-and-report (active-runner) (active-reporters))
          (fail-count @(.-results (active-runner))))))))
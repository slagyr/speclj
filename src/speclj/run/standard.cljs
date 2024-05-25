(ns speclj.run.standard
  (:require [speclj.components :as components]
            [speclj.config :as config]
            [speclj.report.progress] ; so that we can load the default reporter
            [speclj.reporting :as reporting]
            [speclj.results :as results]
            [speclj.running :as running]
            [speclj.tags :as tags]))

(def counter (atom 0))

(deftype StandardRunner [num descriptions results]
  running/Runner
  (run-directories [_this _directories _reporters]
    (js/alert "StandardRunner.run-directories:  I don't know how to do this."))

  (submit-description [_this description]
    (swap! descriptions conj description))

  (run-description [_this description reporters]
    (let [run-results (running/do-description description reporters)]
      (swap! results into run-results)))

  (run-and-report [this reporters]
    (doseq [description (running/filter-focused @descriptions)]
      (running/run-description this description reporters))
    (reporting/report-runs* reporters @results)))

(extend-protocol IPrintWithWriter
  StandardRunner
  (-pr-writer [x writer opts]
    (-write writer (str "#<speclj.run.standard.StandardRunner(num: " (.-num x) ", descriptions: "))
    (-pr-writer @(.-descriptions x) writer opts)
    (-write writer ")>"))
  components/Description
  (-pr-writer [x writer _opts]
    (-write writer (str "#<speclj.component.Description(name: " (.-name x) ")>"))))

(defn new-standard-runner []
  (StandardRunner. (swap! counter inc) (atom []) (atom [])))

(reset! config/default-runner-fn new-standard-runner)
(reset! config/default-runner (new-standard-runner))
(def armed false)

(defn run-specs [& configurations]
  "If evaluated outside the context of a spec run, it will run all the specs that have been evaluated using the default
runner and reporter.  A call to this function is typically placed at the end of a spec file so that all the specs
are evaluated by evaluation the file as a script.  Optional configuration parameters may be passed in:

(run-specs :stacktrace true :color false :reporters [\"documentation\"])"
  (when armed
    (config/with-config
      (merge (dissoc config/default-config :runner) (apply hash-map configurations))
      (fn []
        (if-let [filter-msg (tags/describe-filter)]
          (reporting/report-message* (config/active-reporters) filter-msg))
        (running/run-and-report (config/active-runner) (config/active-reporters))
        (results/fail-count @(.-results (config/active-runner)))))))

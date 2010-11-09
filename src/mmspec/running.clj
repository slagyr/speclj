(ns mmspec.running
  (:use
    [mmspec.exec :only (pass-result fail-result)]
    [mmspec.reporting :only (report-runs report-pass report-fail active-reporter)]))

(defn- eval-components [components]
  (doseq [component components] ((.body component))))

(defn- eval-characteristic [characteristic]
  (let [description @(.description characteristic)]
    (eval-components @(.befores description))
    ((.body characteristic))
    (eval-components @(.afters description))))

(defn- do-characteristic [characteristic reporter]
  (try
    (eval-characteristic characteristic)
    (report-pass reporter)
    (pass-result characteristic)
    (catch Exception e
      (report-fail reporter)
      (fail-result characteristic e))))

(defn- do-characteristics [characteristics description reporter]
  (doall
    (for [characteristic characteristics]
      (do-characteristic characteristic reporter))))

(defn- do-description [description reporter]
  (eval-components @(.before-alls description))
  (let [results (do-characteristics @(.charcteristics description) description reporter)]
    (eval-components @(.after-alls description))
    results))

(defprotocol Runner
  (run [this description reporter])
  (report [this reporter]))

(deftype SingleRunner []
  Runner
  (run [this description reporter]
    (let [results (do-description description reporter)]
      (report-runs reporter results)))
  (report [this reporter]
    ))

(deftype MultiRunner [results]
  Runner
  (run [this description reporter]
    (let [run-results (do-description description reporter)]
      (swap! results (fn [_] into _ run-results))))
  (report [this reporter]
    (report-runs reporter results)))

(declare *runner*)

(defn submit-description [description]
  (if (bound? #'*runner*)
    (run *runner* description (active-reporter))
    (run (SingleRunner.) description (active-reporter))))

(ns speclj.cloverage
  (:require [cloverage.coverage :as coverage]
            [speclj.config :refer [*reporters* *runner*]]
            [speclj.report.documentation]
            [speclj.report.progress :as progress]
            [speclj.results :as results]
            [speclj.run.standard :as standard]
            [speclj.running :refer [run-and-report]]))

;; Assumes that cloverage is already in the classpath.

(defmethod coverage/runner-fn :speclj [_opts]
  (prn "_opts: " _opts)
  (fn [nses]
    (let [results (atom [])
          runner (standard/->StandardRunner (atom []) results)
          reporters [(progress/->ProgressReporter)]]
      (binding [*runner* runner *reporters* reporters]
        (apply require (map symbol nses))
        (run-and-report runner reporters))
      {:errors (results/fail-count @results)})))

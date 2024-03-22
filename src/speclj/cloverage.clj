(ns speclj.cloverage
  (:require [cloverage.coverage :as coverage]
            [speclj.config :refer [*reporters* *runner*]]
            [speclj.report.documentation]
            [speclj.results :as results]
            [speclj.run.standard]
            [speclj.running :refer [run-and-report]])
  (:import (speclj.report.documentation DocumentationReporter)
           (speclj.run.standard StandardRunner)))

(defmethod coverage/runner-fn :speclj [_opts]
  (fn [nses]
    (let [results (atom [])
          runner (StandardRunner. (atom []) results)
          reporters [(DocumentationReporter.)]]
      (binding [*runner* runner *reporters* reporters]
        (apply require (map symbol nses))
        (run-and-report runner reporters))
      {:errors (results/fail-count @results)})))
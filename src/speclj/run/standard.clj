(ns speclj.run.standard
  (:require [clojure.java.io :refer [file]]
            [fresh.core :refer [clj-files-in]]
            [speclj.config :refer [active-reporters active-runner default-runner default-runner-fn config-mappings
                                   default-config *runner* *reporters*]]
            [speclj.reporting :refer [report-runs* report-message*]]
            [speclj.results :refer [fail-count]]
            [speclj.running :refer [do-description run-and-report run-description process-compile-error]]
            [speclj.tags :refer [describe-filter]])
  (:import [speclj.running Runner]))

(defn- load-spec [spec-file]
  (let [src (slurp (.getCanonicalPath spec-file))
        rdr (-> (java.io.StringReader. src) (clojure.lang.LineNumberingPushbackReader.))
        path (.getAbsolutePath spec-file)]
    (clojure.lang.Compiler/load rdr path path)))

(deftype StandardRunner [descriptions results]
  Runner
  (run-directories [this directories reporters]
    (let [dir-files (map file directories)
          files (apply clj-files-in dir-files)
          files (sort files)]
      (binding [*runner* this *reporters* reporters]
        (doseq [file files]
          (try
            (load-spec file)
            (catch java.lang.Throwable e
              (process-compile-error this e))))))
    (run-and-report this reporters)
    (fail-count @results))

  (submit-description [this description]
    (swap! descriptions conj description))

  (run-description [this description reporters]
    (let [run-results (do-description description reporters)]
      (swap! results into run-results)))

  (run-and-report [this reporters]
    (doseq [description @descriptions]
      (run-description this description reporters))
    (report-runs* reporters @results)))

(defn new-standard-runner []
  (StandardRunner. (atom []) (atom [])))

(reset! default-runner-fn new-standard-runner)

(defn run-specs [& configurations]
  (when (identical? (active-runner) @default-runner) ; Solo file run?
    (let [config (apply hash-map configurations)
          config (merge (dissoc default-config :runner) config)]
      (with-bindings (config-mappings config)
        (if-let [filter-msg (describe-filter)]
          (report-message* (active-reporters) filter-msg))
        (run-and-report (active-runner) (active-reporters))
        (reset! default-runner (@default-runner-fn))))))

(reset! default-runner (@default-runner-fn))

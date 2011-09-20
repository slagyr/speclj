(ns speclj.run.standard
  (:use
    [speclj.running :only (do-description run-and-report run-description clj-files-in)]
    [speclj.reporting :only (report-runs*)]
    [speclj.exec :only (fail-count)]
    [speclj.config :only (default-runner *runner* *reporters*)])
  (:import
    [speclj.running Runner]))

(defn- load-spec [spec-file]
  (try
  (let [src (slurp (.getCanonicalPath spec-file))
        rdr (-> (java.io.StringReader. src) (clojure.lang.LineNumberingPushbackReader.))]
    (clojure.lang.Compiler/load rdr (.getParent spec-file) (.getName spec-file)))))

(deftype StandardRunner [descriptions results]
  Runner
  (run-directories [this directories reporters]
    (let [files (apply clj-files-in directories)
          files (sort files)]
      (binding [*runner* this *reporters* reporters]
        (doseq [file files]
          (load-spec file))))
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

(swap! default-runner (fn [_] (new-standard-runner)))

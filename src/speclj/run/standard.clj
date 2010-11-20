(ns speclj.run.standard
  (:use
    [speclj.running :only (do-description report default-runner *runner* clj-files-in)]
    [speclj.reporting :only (report-runs *reporter*)]
    [speclj.exec :only (fail-count)])
  (:import
    [speclj.running Runner]))

(deftype StandardRunner [results]
  Runner
  (run-directories [this directories reporter]
    (let [files (apply clj-files-in directories)]
      (binding [*runner* this *reporter* reporter]
        (doseq [file files]
          (load-string (slurp (.getCanonicalPath file))))))
    (report this reporter)
    (fail-count @results))
  
  (run-description [this description reporter]
    (let [run-results (do-description description reporter)]
      (swap! results into run-results)))

  (report [this reporter]
    (report-runs reporter @results)))

(defn new-standard-runner []
  (StandardRunner. (atom [])))

(swap! default-runner (fn [_] (new-standard-runner)))

(ns speclj.run.standard
  (:use
    [speclj.running :only (do-description run-and-report run-description)]
    [speclj.reporting :only (report-runs*)]
    [speclj.results :only (fail-count)]
    [speclj.config :only (default-runner *runner* *reporters*)]
    [fresh.core :only (clj-files-in)]
    [clojure.java.io :only (file)])
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
    (let [dir-files (map file directories)
          files (apply clj-files-in dir-files)
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
    (reset! results [])
    (doseq [description @descriptions]
      (run-description this description reporters))
    (report-runs* reporters @results)))

(defn new-standard-runner []
  (StandardRunner. (atom []) (atom [])))

(reset! default-runner (new-standard-runner))

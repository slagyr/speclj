(ns speclj.runner
  (:use
    [speclj.running :only (report *runner*)]
    [speclj.reporting :only (active-reporter)]
    [speclj.run.standard :only (new-standard-runner)]
    [speclj.run.vigilance :only (watch)])
  (:import (java.io File)))

(defn find-test-files [dirname]
  (let [files (file-seq (File. dirname))
        file-regex #".*\.clj"]
    (filter #(re-matches file-regex (.getName %)) files)))

;(binding [*runner* (new-standard-runner)]
;  (doseq [file (find-test-files (first *command-line-args*))]
;    (load-string (slurp (.getCanonicalPath file))))
;  (report *runner* (active-reporter))
;  )


(watch (first *command-line-args*))

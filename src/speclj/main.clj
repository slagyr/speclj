(ns speclj.runner
  (:use
    [speclj.running :only (*runner* new-multi-runner report)]
    [speclj.runner.vigilant :only (watch)]
    [speclj.reporting :only (active-reporter)])
  (:import (java.io File)))

(defn find-test-files [dirname]
  (let [files (file-seq (File. dirname))
        file-regex #".*\.clj"]
    (filter #(re-matches file-regex (.getName %)) files)))

(binding [*runner* (new-multi-runner)]
  (doseq [file (find-test-files (first *command-line-args*))]
    (load-string (slurp (.getCanonicalPath file))))
  (report *runner* (active-reporter))
  )


;(watch (first *command-line-args*))

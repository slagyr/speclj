(ns speclj.report.progress-spec
  (:use
    [speclj.core]
    [speclj.report.progress :only (new-progress-reporter)]
    [speclj.reporting]
    [speclj.exec :only (pass-result fail-result)]
    [speclj.components :only (new-description new-characteristic)]
    [clojure.string :only (split-lines)])
  (:import
    [speclj SpecFailure]
    [java.io ByteArrayOutputStream OutputStreamWriter]))

(defn to-s [output]
  (String. (.toByteArray output)))

(describe "Progress Reporter"
  (with output (ByteArrayOutputStream.))
  (with writer (OutputStreamWriter. @output))
  (with reporter (new-progress-reporter))
  (around [spec] (binding [*out* @writer] (spec)))

  (it "reports pass"
    (report-pass @reporter nil)
    (should= "." (to-s @output)))

  (it "reports fail"
    (report-fail @reporter nil)
    (should= "F" (to-s @output)))

  (it "doesnt report description"
    (report-description @reporter nil)
    (should= "" (to-s @output)))

  (it "reports passing run results"
    (let [result1 (pass-result nil 0.1)
          result2 (pass-result nil 0.02)
          result3 (pass-result nil 0.003)
          results [result1 result2 result3]
          _ (report-runs @reporter results)
          lines (split-lines (to-s @output))]
      (should= 5 (count lines))
      (should= "" (lines 0))
      (should= "" (lines 1))
      (should= "Finished in 0.12300 seconds" (lines 2))
      (should= "" (lines 3))
      (should= "3 examples, 0 failures" (lines 4))))


  (it "reports failing run results"
    (let [description (new-description "Crazy")
          char1 (new-characteristic "flips" description "flip")
          char2 (new-characteristic "spins" description "spin")
          char3 (new-characteristic "dives" description "dive")
          result1 (fail-result char1 0.3 (SpecFailure. "Expected flips"))
          result2 (fail-result char2 0.02 (SpecFailure. "Expected spins"))
          result3 (fail-result char3 0.001 (SpecFailure. "Expected dives"))
          results [result1 result2 result3]
          _ (report-runs @reporter results)
          lines (split-lines (to-s @output))]
      (should= 20 (count lines))
      (should= "" (lines 0))
      (should= "" (lines 1))
      (should= "1)" (lines 2))
      (should= "'Crazy flips' FAILED" (lines 3))
      (should= "Expected flips" (lines 4))
;      (should= "/Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:54" (lines 5))
      (should= "" (lines 6))
      (should= "2)" (lines 7))
      (should= "'Crazy spins' FAILED" (lines 8))
      (should= "Expected spins" (lines 9))
;      (should= "/Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:55" (lines 10))
      (should= "" (lines 11))
      (should= "3)" (lines 12))
      (should= "'Crazy dives' FAILED" (lines 13))
      (should= "Expected dives" (lines 14))
;      (should= "/Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:56" (lines 15))
      (should= "" (lines 16))
      (should= "Finished in 0.32100 seconds" (lines 17))
      (should= "" (lines 18))
      (should= "3 examples, 3 failures" (lines 19))))
  )

(conclude-single-file-run)
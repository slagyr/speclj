(ns speclj.report.progress-spec
  (:use
    [speclj.core]
    [speclj.report.progress :only (new-progress-reporter full-name print-summary)]
    [speclj.reporting]
    [speclj.exec :only (pass-result fail-result pending-result)]
    [speclj.components :only (new-description new-characteristic install)]
    [speclj.config :only (*color?*)]
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
  (around [spec] (binding [*out* @writer
                           *color?* false] (spec)))

  (it "reports pass"
    (report-pass @reporter nil)
    (should= "." (to-s @output)))

  (it "reports pass in green with color"
    (binding [*color?* true]
      (report-pass @reporter nil)
      (should= (green ".") (to-s @output))))

  (it "reports pending"
    (report-pending @reporter nil)
    (should= "*" (to-s @output)))

  (it "reports pending in yellow with color"
    (binding [*color?* true]
      (report-pending @reporter nil)
      (should= (yellow "*") (to-s @output))))

  (it "reports fail"
    (report-fail @reporter nil)
    (should= "F" (to-s @output)))

  (it "reports fail in red with color"
    (binding [*color?* true]
      (report-fail @reporter nil)
      (should= (red "F") (to-s @output))))

  (it "doesnt report description"
    (report-description @reporter nil)
    (should= "" (to-s @output)))

  (it "reports passing run results"
    (binding [*color?* true]
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
        (should= (green "3 examples, 0 failures") (lines 4)))))

  (it "reports failing run results"
    (binding [*color?* true]
      (let [description (new-description "Crazy" *ns*)
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
        (should= (red "'Crazy flips' FAILED") (lines 3))
        (should= (red "Expected flips") (lines 4))
;        (should= "/Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:67" (lines 5))
        (should= "" (lines 6))
        (should= "2)" (lines 7))
        (should= (red "'Crazy spins' FAILED") (lines 8))
        (should= (red "Expected spins") (lines 9))
        ;      (should= "/Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:55" (lines 10))
        (should= "" (lines 11))
        (should= "3)" (lines 12))
        (should= (red "'Crazy dives' FAILED") (lines 13))
        (should= (red "Expected dives") (lines 14))
        ;      (should= "/Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:56" (lines 15))
        (should= "" (lines 16))
        (should= "Finished in 0.32100 seconds" (lines 17))
        (should= "" (lines 18))
        (should= (red "3 examples, 3 failures") (lines 19)))))

  (it "reports pending run results"
    (binding [*color?* true]
      (let [result1 (pass-result nil 0.1)
            result2 (pass-result nil 0.02)
            result3 (pending-result nil 0.003)
            results [result1 result2 result3]
            _ (print-summary results)
            lines (split-lines (to-s @output))]
        (should= (yellow "3 examples, 0 failures, 1 pending") (last lines)))))

  (it "can calculate the full name of a characteristic"
    (let [outer (new-description "Outer" *ns*)
          inner (new-description "Inner" *ns*)
          char (new-characteristic "char" inner "char")]
      (install inner outer)
      (should= "Outer Inner char" (full-name char))))
  )

(run-specs)

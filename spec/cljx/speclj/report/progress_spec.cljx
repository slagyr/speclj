(ns speclj.report.progress-spec
  (#+clj :require #+cljs :require-macros ;cljs-macros
            [speclj.core :refer [around before context describe it should should= with]]
            #+clj [speclj.platform-clj-macros :refer [new-exception new-failure new-pending]]
            #+cljs [speclj.platform-cljs-macros :refer [new-exception new-failure new-pending]]
         )
  (:require [clojure.string :as str]
            ;cljs-include [goog.string] ;cljs bug?
            #+cljs [goog.string]
            [speclj.components :refer [new-description new-characteristic install]]
            [speclj.config :refer [*color?* *full-stack-trace?*]]
            [speclj.platform :refer [format-seconds]]
            [speclj.report.progress :refer [new-progress-reporter full-name print-summary print-pendings print-errors]]
            [speclj.reporting :refer [report-description report-pass report-pending
                                      report-fail report-error red green yellow grey report-runs]]
            [speclj.results :refer [pass-result fail-result pending-result error-result]]
            [speclj.run.standard :refer [run-specs]]))

(describe "Progress Reporter"
  (with reporter (new-progress-reporter))
  (around [spec] (binding [*color?* false] (spec)))

  (it "reports pass"
    (should= "."
      (with-out-str (report-pass @reporter nil))))

  (it "reports pass in green with color"
    (binding [*color?* true]
      (should= (green ".")
        (with-out-str (report-pass @reporter nil)))))

  (it "reports pending"
    (should= "*"
      (with-out-str (report-pending @reporter nil))))

  (it "reports pending in yellow with color"
    (binding [*color?* true]
      (should= (yellow "*")
        (with-out-str (report-pending @reporter nil)))))

  (it "reports fail"
    (should= "F"
      (with-out-str (report-fail @reporter nil))))

  (it "reports fail in red with color"
    (binding [*color?* true]
      (should= (red "F")
        (with-out-str (report-fail @reporter nil)))))

  (it "doesnt report description"
    (should= ""
      (with-out-str (report-description @reporter nil))))

  (it "reports errors"
    (should= "E"
      (with-out-str (report-error @reporter (new-exception "Compilation failed")))))

  (it "reports passing run results"
    (binding [*color?* true]
      (let [result1 (pass-result nil 0.1)
            result2 (pass-result nil 0.02)
            result3 (pass-result nil 0.003)
            results [result1 result2 result3]
            output (with-out-str (report-runs @reporter results))
            lines (str/split-lines output)]
        (should= 4 (count lines))
        (should= "" (nth lines 0))
        (should= "" (nth lines 1))
        (should= (str "Finished in " (format-seconds 0.123) " seconds") (nth lines 2))
        (should= (green "3 examples, 0 failures") (nth lines 3)))))

  (it "reports failing run results"
    (binding [*color?* true]
      (let [description (new-description "Crazy" "some.ns")
            char1 (new-characteristic "flips" description "flip")
            char2 (new-characteristic "spins" description "spin")
            char3 (new-characteristic "dives" description "dive")
            result1 (fail-result char1 0.3 (new-failure "Expected flips"))
            result2 (fail-result char2 0.02 (new-failure "Expected spins"))
            result3 (fail-result char3 0.001 (new-failure "Expected dives"))
            results [result1 result2 result3]
            lines (str/split-lines (with-out-str (report-runs @reporter results)))]
        (should= 18 (count lines))
        (should= "" (nth lines 0))
        (should= "Failures:" (nth lines 2))
        (should= "" (nth lines 3))
        (should= "  1) Crazy flips" (nth lines 4))
        (should= (red "     Expected flips") (nth lines 5))
        ;        (should= "/Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:67" (nth lines 6))
        (should= "" (nth lines 7))
        (should= "  2) Crazy spins" (nth lines 8))
        (should= (red "     Expected spins") (nth lines 9))
        ;      (should= "/Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:55" (nth lines 10))
        (should= "" (nth lines 11))
        (should= "  3) Crazy dives" (nth lines 12))
        (should= (red "     Expected dives") (nth lines 13))
        ;      (should= "/Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:56" (nth lines 14))
        (should= "" (nth lines 15))
        (should= (str "Finished in " (format-seconds 0.321) " seconds") (nth lines 16))
        (should= (red "3 examples, 3 failures") (nth lines 17)))))

  (it "reports pending run results"
    (binding [*color?* true]
      (let [description (new-description "Crazy" "some.ns")
            char1 (new-characteristic "flips" description "flip")
            result1 (pass-result char1 0.1)
            result2 (pass-result char1 0.02)
            result3 (pending-result char1 0.003 (new-pending "Blah"))
            results [result1 result2 result3]
            lines (str/split-lines (with-out-str (print-summary results)))]
        (should= (yellow "3 examples, 0 failures, 1 pending") (last lines)))))

  (it "reports pending summary"
    (let [description (new-description "Crazy" "some.ns")
          char1 (new-characteristic "flips" description "flip")
          result1 (pending-result char1 0.3 (new-pending "Not Yet Implemented"))
          lines (str/split-lines (with-out-str (print-pendings [result1])))]
      (should= 6 (count lines))
      (should= "" (nth lines 0))
      (should= "Pending:" (nth lines 1))
      (should= "" (nth lines 2))
      (should= (yellow "  Crazy flips") (nth lines 3))
      (should= (grey "    ; Not Yet Implemented") (nth lines 4))
      ;      (should= (grey "    ; /Users/micahmartin/Projects/clojure/speclj/spec/speclj/report/progress_spec.clj:117") (nth lines 5))
      ))

  (it "reports error run results"
    (binding [*color?* true]
      (let [description (new-description "Crazy" "some.ns")
            char1 (new-characteristic "flips" description "flip")
            result1 (pass-result char1 0.1)
            result2 (pass-result char1 0.02)
            result3 (error-result (new-exception "blah"))
            results [result1 result2 result3]
            lines (str/split-lines (with-out-str (print-summary results)))]
        (should= (red "3 examples, 0 failures, 1 errors") (last lines)))))

  (it "reports error summary"
    (binding [*full-stack-trace?* false]
      (let [description (new-description "Crazy" "some.ns")
            char1 (new-characteristic "flips" description "flip")
            result1 (error-result (new-exception "blah"))
            lines (str/split-lines (with-out-str (print-errors [result1])))]
        (should (> (count lines) 3))
        (should= "" (nth lines 0))
        (should= "Errors:" (nth lines 1))
        (should= "" (nth lines 2))
        (should= (str "  1) " (new-exception "blah")) (nth lines 3)))))

  (it "can calculate the full name of a characteristic"
    (let [outer (new-description "Outer" "some.ns")
          inner (new-description "Inner" "some.ns")
          char (new-characteristic "char" inner "char")]
      (install inner outer)
      (should= "Outer Inner char" (full-name char))))
  )

(run-specs :stacktrace true)

(ns speclj.report.documentation-spec
  (:require [clojure.string :refer [split-lines]]
            [speclj.components :refer [new-description new-characteristic install]]
            [speclj.config :refer [*color?*]]
            [speclj.core :refer :all]
            [speclj.report.documentation :refer [new-documentation-reporter]]
            [speclj.reporting :refer :all]
            [speclj.results :refer [pass-result fail-result pending-result error-result]]
            [speclj.util :refer [endl]])
  (:import [speclj SpecPending]
           [java.io ByteArrayOutputStream OutputStreamWriter]))

(defn to-s [output]
  (String. (.toByteArray output)))

(describe "Speccdoc Reporter"
  (with output (ByteArrayOutputStream.))
  (with writer (OutputStreamWriter. @output))
  (with reporter (new-documentation-reporter))
  (with description (new-description "Verbosity" *ns*))
  (around [spec] (binding [*out* @writer
                           *color?* true] (spec)))

  (it "reports descriptions"
    (report-description @reporter @description)
    (should= (str endl "Verbosity" endl) (to-s @output)))

  (it "reports errors"
    (report-error @reporter (error-result (Exception. "Compilation failed")))
    (should= (str (red "java.lang.Exception: Compilation failed") "\n") (to-s @output)))

  (it "reports pass"
    (let [characteristic (new-characteristic "says pass" @description "pass")
          result (pass-result characteristic 1)]
      (report-pass @reporter result)
      (should= (str (green "- says pass") endl) (to-s @output))))

  (it "reports pending"
    (let [characteristic (new-characteristic "pending!" `(pending))
          result (pending-result characteristic 1 (SpecPending. "some reason for pendiness"))]
      (report-pending @reporter result)
      (should= (str (yellow "- pending! (PENDING: some reason for pendiness)") endl) (to-s @output))))

  (it "reports fail"
    (let [characteristic (new-characteristic "says fail" @description "fail")
          result (fail-result characteristic 2 (AssertionError. "blah"))]
      (report-fail @reporter result)
      (should= (str (red "- says fail (FAILED)") endl) (to-s @output))))

  (context "with nested description"
    (with nested-description (new-description "Nesting" *ns*))
    (before (install @nested-description @description))

    (it "indents nested description"
      (report-description @reporter @nested-description)
      (should= (str "  Nesting" endl) (to-s @output)))

    (it "reports nested pass"
      (let [characteristic (new-characteristic "nested pass" @nested-description "pass")
            result (pass-result characteristic 1)]
        (report-pass @reporter result)
        (should= (str (green "  - nested pass") endl) (to-s @output))))

    (it "reports nested failure"
      (let [characteristic (new-characteristic "nested fail" @nested-description "fail")
            result (fail-result characteristic 2 (AssertionError. "blah"))]
        (report-fail @reporter result)
        (should= (str (red "  - nested fail (FAILED)") endl) (to-s @output))))
    )

  )


(run-specs)

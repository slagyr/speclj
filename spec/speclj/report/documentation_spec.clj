(ns speclj.report.documentation-spec
  (:use
    [speclj.core]
    [speclj.report.documentation :only (new-documentation-reporter)]
    [speclj.reporting]
    [speclj.results :only (pass-result fail-result pending-result)]
    [speclj.components :only (new-description new-characteristic install)]
    [clojure.string :only (split-lines)]
    [speclj.util :only (endl)]
    [speclj.config :only (*color?*)])
  (:import
    [speclj SpecFailure SpecPending]
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

  (it "doesnt report errors"
    (report-error @reporter (Exception. "Compilation failed"))
    (should= "" (to-s @output)))

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
          result (fail-result characteristic 2 (SpecFailure. "blah"))]
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
            result (fail-result characteristic 2 (SpecFailure. "blah"))]
        (report-fail @reporter result)
        (should= (str (red "  - nested fail (FAILED)") endl) (to-s @output))))
    )

  )


(run-specs)

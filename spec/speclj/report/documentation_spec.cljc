(ns speclj.report.documentation-spec
  (:require #?(:cljs [goog.string]) ;cljs bug?
            [speclj.core #?(:clj :refer :cljs :refer-macros) [before context describe it should= with -new-exception -new-failure -new-pending]]
            [speclj.spec-helper #?(:clj :refer :cljs :refer-macros) [test-exported-meta]]
            [speclj.components :refer [new-description new-characteristic install]]
            [speclj.platform :refer [endl]]
            [speclj.report.documentation :as sut]
            [speclj.reporting :refer [report-description report-pass report-pending
                                      report-fail report-error red green yellow]]
            [speclj.results :refer [pass-result fail-result pending-result error-result]]
            [speclj.run.standard :as standard]))

(describe "Speccdoc Reporter"
  (with reporter (sut/new-documentation-reporter))
  (with description (new-description "Verbosity" false "some-ns"))

  (it "reports descriptions"
    (should= (str endl "Verbosity" endl)
             (with-out-str (report-description @reporter @description))))

  (it "reports focused descriptions"
    (let [description (new-description "Verbosity" true "some-ns")]
      (should= (str endl "Verbosity " (yellow "[FOCUS]") endl)
               (with-out-str (report-description @reporter description)))))

  (it "reports errors"
    (should= (str (red (str (-new-exception "Compilation failed"))) "\n")
             (with-out-str (report-error @reporter (error-result (-new-exception "Compilation failed"))))))

  (it "reports pass"
    (let [characteristic (new-characteristic "says pass" @description "pass" false)
          result         (pass-result characteristic 1)]
      (should= (str (green "- says pass") endl)
               (with-out-str (report-pass @reporter result)))))

  (it "reports focused pass"
    (let [characteristic (new-characteristic "says pass" @description #() true)
          result         (pass-result characteristic 1)]
      (should= (str (green "- says pass") " " (yellow "[FOCUS]") endl)
               (with-out-str (report-pass @reporter result)))))

  (it "reports pending"
    (let [characteristic (new-characteristic "pending!" nil `(pending) false)
          result         (pending-result characteristic 1 (-new-pending "some reason for pendiness"))]
      (should= (str (yellow "- pending! (PENDING: some reason for pendiness)") endl)
               (with-out-str (report-pending @reporter result)))))

  (it "reports fail"
    (let [characteristic (new-characteristic "says fail" @description "fail" false)
          result         (fail-result characteristic 2 (-new-failure "blah"))]
      (should= (str (red "- says fail (FAILED)") endl)
               (with-out-str (report-fail @reporter result)))))

  (it "reports focused fail"
    (let [characteristic (new-characteristic "says fail" @description #() true)
          result         (fail-result characteristic 2 (-new-failure "blah"))]
      (should= (str (red "- says fail (FAILED)") " " (yellow "[FOCUS]") endl)
               (with-out-str (report-fail @reporter result)))))

  (context "exporting"
    (test-exported-meta sut/new-documentation-reporter)
    )

  (context "with nested description"
    (with nested-description (new-description "Nesting" false "some.ns"))
    (before (install @nested-description @description))

    (it "indents nested description"
      (should= (str "  Nesting" endl)
               (with-out-str (report-description @reporter @nested-description))))

    (it "reports nested pass"
      (let [characteristic (new-characteristic "nested pass" @nested-description "pass" false)
            result         (pass-result characteristic 1)]
        (should= (str (green "  - nested pass") endl)
                 (with-out-str (report-pass @reporter result)))))

    (it "reports nested failure"
      (let [characteristic (new-characteristic "nested fail" @nested-description "fail" false)
            result         (fail-result characteristic 2 (-new-failure "blah"))]
        (should= (str (red "  - nested fail (FAILED)") endl)
                 (with-out-str (report-fail @reporter result)))))
    )
  )

(standard/run-specs)

(ns speclj.report.documentation-spec
  #?(:cljs (:require-macros [speclj.report.documentation-spec :refer [with-profiler-off with-profiler-on]]))
  (:require #?(:cljs [goog.string]) ;cljs bug?
            [speclj.core #?(:cljs :refer-macros :default :refer) [before context describe it should= with -new-exception -new-failure -new-pending pending]]
            [speclj.spec-helper #?(:cljs :refer-macros :default :refer) [test-exported-meta]]
            [speclj.components :refer [new-description new-characteristic install]]
            [speclj.platform :refer [endl] :as platform]
            [speclj.config :as config]
            [speclj.report.documentation :as sut]
            [speclj.reporting :refer [report-description report-pass report-pending
                                      report-fail report-error red green yellow]]
            [speclj.results :refer [pass-result fail-result pending-result error-result]]))

(defmacro with-profiler-off [& body]
  `(binding [config/*profile?* false] ~@body))

(defmacro with-profiler-on [& body]
  `(binding [config/*profile?* true] ~@body))

(defn seconds-str [seconds]
  (str "[" (platform/format-seconds seconds) "s] "))

(defn seconds-prefix [seconds]
  (yellow (seconds-str seconds)))

(def profiler-whitespace
  (let [spaces (count (seconds-str 0))]
    (apply str (repeat spaces " "))))

(describe "Speccdoc Reporter"
  (with reporter (sut/new-documentation-reporter))
  (with description (new-description "Verbosity" false "some-ns"))

  (it "reports descriptions"
    (with-profiler-off
      (should= (str endl "Verbosity" endl)
               (with-out-str (report-description @reporter @description)))))

  (it "reports focused descriptions"
    (with-profiler-off
      (let [description (new-description "Verbosity" true "some-ns")]
        (should= (str endl "Verbosity " (yellow "[FOCUS]") endl)
                 (with-out-str (report-description @reporter description))))))

  (it "reports profiled descriptions"
    (with-profiler-on
      (let [description (new-description "Verbosity" true "some-ns")]
        (should= (str endl profiler-whitespace "Verbosity " (yellow "[FOCUS]") endl)
                 (with-out-str (report-description @reporter description))))))

  (it "reports focused and profiled descriptions"
    (with-profiler-on
      (let [description (new-description "Verbosity" true "some-ns")]
        (should= (str endl profiler-whitespace "Verbosity " (yellow "[FOCUS]") endl)
                 (with-out-str (report-description @reporter description))))))

  (it "reports errors"
    (should= (str (red (str (-new-exception "Compilation failed"))) "\n")
             (with-out-str (report-error @reporter (error-result (-new-exception "Compilation failed"))))))

  (it "reports pass"
    (with-profiler-off
      (let [characteristic (new-characteristic "says pass" @description "pass" false)
            result         (pass-result characteristic 1 0)]
        (should= (str (green "- says pass") endl)
                 (with-out-str (report-pass @reporter result))))))

  (it "reports focused pass"
    (with-profiler-off
      (let [characteristic (new-characteristic "says pass" @description #() true)
            result         (pass-result characteristic 1 0)]
        (should= (str (green "- says pass") " " (yellow "[FOCUS]") endl)
                 (with-out-str (report-pass @reporter result))))))

  (it "reports profiled pass"
    (with-profiler-on
      (let [characteristic (new-characteristic "says pass" @description #() false)
            result         (pass-result characteristic 1 0)]
        (should= (str (seconds-prefix 1) (green "- says pass") endl)
                 (with-out-str (report-pass @reporter result))))))

  (it "reports profiled and focused pass"
    (with-profiler-on
      (let [characteristic (new-characteristic "says pass" @description #() true)
            result         (pass-result characteristic 1 0)]
        (should= (str (seconds-prefix 1)
                      (green "- says pass")
                      " " (yellow "[FOCUS]")
                      endl)
                 (with-out-str (report-pass @reporter result))))))

  (it "reports pending"
    (with-profiler-off
      (let [characteristic (new-characteristic "pending!" nil `(pending) false)
            result         (pending-result characteristic 1 (-new-pending "some reason for pendiness"))]
        (should= (str (yellow "- pending! (PENDING: some reason for pendiness)") endl)
                 (with-out-str (report-pending @reporter result))))))

  (it "reports profiled pending"
    (with-profiler-on
      (let [characteristic (new-characteristic "pending!" nil `(pending) false)
            result         (pending-result characteristic 1 (-new-pending "some reason for pendiness"))]
        (should= (str (seconds-prefix 1)
                      (yellow "- pending! (PENDING: some reason for pendiness)")
                      endl)
                 (with-out-str (report-pending @reporter result))))))

  (it "reports profiled and pending"
    (with-profiler-on
      (let [characteristic (new-characteristic "pending!" nil `(pending) false)
            result         (pending-result characteristic 1 (-new-pending "some reason for pendiness"))]
        (should= (str (seconds-prefix 1)
                      (yellow "- pending! (PENDING: some reason for pendiness)")
                      endl)
                 (with-out-str (report-pending @reporter result))))))

  (it "reports fail"
    (with-profiler-off
      (let [characteristic (new-characteristic "says fail" @description "fail" false)
            result         (fail-result characteristic 2 (-new-failure "blah") 0)]
        (should= (str (red "- says fail (FAILED)") endl)
                 (with-out-str (report-fail @reporter result))))))

  (it "reports profiled fail"
    (with-profiler-on
      (let [characteristic (new-characteristic "says fail" @description "fail" false)
            result         (fail-result characteristic 2 (-new-failure "blah") 0)]
        (should= (str (seconds-prefix 2)
                      (red "- says fail (FAILED)")
                      endl)
                 (with-out-str (report-fail @reporter result))))))

  (it "reports focused fail"
    (with-profiler-off
      (let [characteristic (new-characteristic "says fail" @description #() true)
            result         (fail-result characteristic 2 (-new-failure "blah") 0)]
        (should= (str (red "- says fail (FAILED)") " " (yellow "[FOCUS]") endl)
                 (with-out-str (report-fail @reporter result))))))

  (it "reports profiled and focused fail"
    (with-profiler-on
      (let [characteristic (new-characteristic "says fail" @description "fail" true)
            result         (fail-result characteristic 2 (-new-failure "blah") 0)]
        (should= (str (seconds-prefix 2)
                      (red "- says fail (FAILED)")
                      " " (yellow "[FOCUS]")
                      endl)
                 (with-out-str (report-fail @reporter result))))))

  (context "exporting"
    (test-exported-meta sut/new-documentation-reporter)
    )

  (context "with nested description"
    (with nested-description (new-description "Nesting" false "some.ns"))
    (before (install @nested-description @description))

    (it "indents nested description"
      (with-profiler-off
        (should= (str "  Nesting" endl)
                 (with-out-str (report-description @reporter @nested-description)))))

    (it "indents nested profiled description"
      (with-profiler-on
        (should= (str profiler-whitespace "  Nesting" endl)
                 (with-out-str (report-description @reporter @nested-description)))))

    (it "reports nested pass"
      (with-profiler-off
        (let [characteristic (new-characteristic "nested pass" @nested-description "pass" false)
              result         (pass-result characteristic 1 0)]
          (should= (str (green "  - nested pass") endl)
                   (with-out-str (report-pass @reporter result))))))

    (it "reports nested failure"
      (with-profiler-off
        (let [characteristic (new-characteristic "nested fail" @nested-description "fail" false)
              result         (fail-result characteristic 2 (-new-failure "blah") 0)]
          (should= (str (red "  - nested fail (FAILED)") endl)
                   (with-out-str (report-fail @reporter result))))))
    )
  )

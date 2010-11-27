(ns speclj.report.specdoc-spec
  (:use
    [speclj.core]
    [speclj.report.specdoc :only (new-specdoc-reporter)]
    [speclj.reporting]
    [speclj.exec :only (pass-result fail-result)]
    [speclj.components :only (new-description new-characteristic)]
    [clojure.string :only (split-lines)]
    [speclj.util :only (endl)])
  (:import
    [speclj SpecFailure]
    [java.io ByteArrayOutputStream OutputStreamWriter]))

(defn to-s [output]
  (String. (.toByteArray output)))

(describe "Speccdoc Reporter"
  (with output (ByteArrayOutputStream.))
  (with writer (OutputStreamWriter. @output))
  (with reporter (new-specdoc-reporter))
  (with description (new-description "Verbosity"))
  (around [spec] (binding [*out* @writer] (spec)))

  (it "reports descriptions"
    (report-description @reporter @description)
    (should= (str endl "Verbosity" endl) (to-s @output)))

  (it "reports pass"
    (let [characteristic (new-characteristic "says pass" @description "pass")
          result (pass-result characteristic 1)]
      (report-pass @reporter result)
      (should= (str "- says pass" endl) (to-s @output))))

  (it "reports fail"
    (let [characteristic (new-characteristic "says fail" @description "fail")
          result (fail-result characteristic 2 (SpecFailure. "blah"))]
      (report-fail @reporter result)
      (should= (str "- says fail (FAILED)" endl) (to-s @output))))

  )


(run-specs)
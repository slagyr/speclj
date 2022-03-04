(ns speclj.report.clojure-test-spec
  (:require                                                 ;cljs-macros
    [clojure.string :as str]
    [clojure.test]
    ;cljs-include [goog.string] ;cljs bug?
    [speclj.components :refer [new-characteristic new-description]]
    [speclj.core :refer [-new-exception -new-failure describe it should-contain should= with]]
    [speclj.report.clojure-test :refer [new-clojure-test-reporter]]
    [speclj.reporting :refer [report-error report-fail report-pass report-pending report-runs]]
    [speclj.results :refer [error-result fail-result]]
    [speclj.run.standard :refer [run-specs]]))

(defmacro with-test-out-str [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [clojure.test/*test-out* s#]
       ~@body
       (str s#))))

(describe "Clojure Test Reporter"
  (with reporter (new-clojure-test-reporter))
  (with a-failure (fail-result (new-characteristic "flips" (new-description "Crazy" "some.ns") "flip") 0.3 (-new-failure "Expected flips")))
  (with an-error (error-result (-new-exception "Compilation failed")))

  (it "reports pass"
    (with-test-out-str (report-pass @reporter nil))
    (should= 1
             (:pass (deref (.report-counters @reporter)))))

  (it "reports pending"
    (with-test-out-str (report-pending @reporter nil))
    (should= 1
             (:pending (deref (.report-counters @reporter)))))

  (it "reports fail"
    (with-test-out-str (report-fail @reporter @a-failure))
    (should= 1
             (:fail (deref (.report-counters @reporter)))))

  (it "reports failure message"
    (let [lines (str/split-lines (with-test-out-str (report-fail @reporter @a-failure)))]
      (should-contain "FAIL in (Crazy flips)" (nth lines 1))
      (should-contain "Expected flips" (nth lines 2))))

  (it "reports error"
    (with-test-out-str (report-error @reporter @an-error))
    (should= 1
             (:error (deref (.report-counters @reporter)))))

  (it "reports error message"
    (let [lines (str/split-lines (with-test-out-str (report-error @reporter @an-error)))]
      (should-contain "ERROR in (unknown)" (nth lines 1))
      (should-contain "Compilation failed" (nth lines 2))
      (should-contain "actual: java.lang.Exception: Compilation failed" (nth lines 4))))

  (it "reports run results"
    (with-test-out-str
      (report-pending @reporter nil)
      (report-pass @reporter nil)
      (report-fail @reporter @a-failure)
      (report-error @reporter @an-error))
    (let [output (with-test-out-str (report-runs @reporter nil))
          lines  (str/split-lines output)]
      (should= 3 (count lines))
      (should= "Ran 3 tests containing 3 assertions." (nth lines 1))
      (should= "1 failures, 1 errors." (nth lines 2))))
  )

(run-specs)

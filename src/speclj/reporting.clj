(ns speclj.reporting
  (:use
    [speclj.exec :only (pass? fail?)]))

(defprotocol Reporter
  (report-pass [this])
  (report-fail [this])
  (report-runs [this results]))

(defn failure-source [exception]
  (let [source (nth (.getStackTrace exception) 0)]
    (str (.getAbsolutePath (java.io.File. (.getFileName source))) ":" (.getLineNumber source))))

(defn- print-failure [id result]
  (let [characteristic (.characteristic result)
        description @(.description characteristic)
        failure (.failure result)]
    (println)
    (println (str id ")"))
    (println (str "'" (.name description) (.name characteristic) "' FAILED"))
    (println (.getMessage failure))
    (println (failure-source failure))
;    (.printStackTrace failure)
    ))

(defn- print-failures [results]
  (println)
  (let [failures (filter fail? results)]
    (doseq [failure failures id (range 1 (inc (count failures)))]
      (print-failure id failure))))

(defn- tally-time [results]
  (loop [tally 0.0 results results]
    (if (seq results)
      (recur (+ tally (.seconds (first results))) (rest results))
      tally)))

(def seconds-format (java.text.DecimalFormat. "0.00000"))

(defn- print-duration [results]
  (println)
  (println "Finished in" (.format seconds-format (tally-time results)) "seconds"))

(defn- print-tally [results]
  (println)
  (let [fails (reduce #(if (fail? %2) (inc %) %) 0 results)]
    (print (count results) "examples," fails "failures")))

(deftype ConsoleReporter []
  Reporter
  (report-pass [this]
    (print "."))
  (report-fail [this]
    (print "F"))
  (report-runs [this results]
    (print-failures results)
    (print-duration results)
    (print-tally results)))

(defn new-console-reporter []
  (ConsoleReporter.))

(deftype SilentReporter [passes fails results]
  Reporter
  (report-pass [this]
    (print "."))
  (report-fail [this]
    (print "F"))
  (report-runs [this results]
    (print-failures results)
    (print-duration results)
    (print-tally results)))

(defn new-silent-reporter []
  (SilentReporter. (atom 0) (atom 0) (atom nil)))

(def *reporter* (new-console-reporter))

(defn active-reporter []
  *reporter*)

;..F.........
;
;1)
;'PrimeFactors should factor 3' FAILED
;expected: [2],
;     got: [3] (using ==)
;/Users/micahmartin/Projects/kata/prime_factors_kata/spec/prime_factors_spec.rb:22:
;
;Finished in 0.316175 seconds
;
;12 examples, 1 failure

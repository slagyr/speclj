(ns mmspec.reporting
  (:use
    [mmspec.exec :only (pass?)]))

(defprotocol Reporter
  (report-pass [this])
  (report-fail [this])
  (report-runs [this results]))

(deftype ConsoleReporter []
  Reporter
  (report-pass [this]
    (print "."))
  (report-fail [this]
    (print "F"))
  (report-runs [this results]
    (println "")
    (println "")
    (let [fail-id (atom 0)]
      (doseq [result results]
        (if-not (pass? result)
          (let [id (swap! fail-id inc)
                characteristic (.characteristic result)
                description @(.description characteristic)]
            (println (str id ")"))
            (println (.name description) (.name characteristic) "FAILED")
            (println (.getMessage (.failure result)))
            (println)))))))

(def *reporter* (ConsoleReporter.))

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

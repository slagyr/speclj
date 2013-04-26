(ns basics-spec.calulator_spec
  (:require [speclj.core :refer :all]))



(describe "Calculator"

  (declare ^:dynamic *the-answer*)

  (before (println "A spec is about to be evaluated"))
  (after (println "A spec has just been evaluated"))
  (before-all (println "May the spec'ing begin!"))
  (after-all (println "That's all folks."))
  (with nice-format (java.text.DecimalFormat. "0.00000"))
  (around [it]
    (binding [*the-answer* 42]
      (it)))


  (it "adds numbers"
    (should= 2 (+ 1 1)))

  (it "formats numbers nicely"
    (should= "3.14159" (.format @nice-format Math/PI)))

  (it "knows the answer"
    (should= 42 *the-answer*))
  )


(run-specs)

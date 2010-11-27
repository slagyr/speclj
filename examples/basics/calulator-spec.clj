(ns basics-spec
  (:use [speclj.core]))


(declare *the-answer*)
(describe "Calculator"

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

    Usage:  java -cp [...] speclj.main [options] [specs*]

      specs  directories specifying which specs to run.

      -r, --runner=<RUNNER>      Use a custom Runner.

                                 Builtin runners:
                                 standard               : (default) Runs all the specs once
                                 vigilant               : Watches for file changes and re-runs affected
                                 specs (used by autotest)
      -f, --reporter=<REPORTER>  Specifies how to report spec results. Ouput will be written to *out*.

                                 Builtin reporters:
                                 silent                 : No output
                                 progress               : (default) Text-based progress bar
                                 specdoc                : Code example doc strings
      -f, --format=<FORMAT>      An alias for reporter.
      -a, --autotest             Alias to use the 'vigilant' runner and 'specdoc' reporter.
      -v, --version              Shows the current speclj version.
      -h, --help                 You're looking at it.

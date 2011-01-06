(ns speclj.reporting-spec
  (:use
    [speclj.core]
    [speclj.reporting]
    [speclj.config :only (*color?* *full-stack-trace?*)]))

(describe "Reporting"
  (context "without color"
    (around [it] (binding [*color?* false] (it)))

    (it "prints all colors as plain text"
      (should= "text" (red "text"))
      (should= "text" (green "text"))
      (should= "text" (grey "text")))
    )

  (context "with color"
    (around [it] (binding [*color?* true] (it)))

    (it "prints in red"
      (should= "\u001b[31mtext\u001b[0m" (red "text")))

    (it "prints in green"
      (should= "\u001b[32mtext\u001b[0m" (green "text")))

    (it "prints in grey"
      (should= "\u001b[90mtext\u001b[0m" (grey "text")))
    )

  (context "with elided stacktrace"
    (around [it] (binding [*full-stack-trace?* false] (it)))

  (it "prints elided stack traces"
    (let [output (java.io.StringWriter.)
          exception (Exception. "Test Exception")
          expected "java.lang.Exception: Test Exception
\tat my_code$start.invoke(my_file.clj:123)
\t... 4 stack levels elided ...
\tat my_code$end.invoke(my_file.clj:124)
"]
      (.setStackTrace exception
        (into-array [
          (StackTraceElement. "my_code$start" "invoke" "my_file.clj" 123)
          (StackTraceElement. "speclj.running$eval_characteristic" "invoke" "running.clj" 22)
          (StackTraceElement. "clojure.lang.RT" "load" "RT.java" 412)
          (StackTraceElement. "clojure.core$load$fn__4511" "invoke" "core.clj" 4905)
          (StackTraceElement. "clojure.core$load" "doInvoke" "core.clj" 4904)
          (StackTraceElement. "my_code$end" "invoke" "my_file.clj" 124)
          ]))
      (print-stack-trace exception output)
      (should= expected (.toString output))))

  (it "prints elided stack traces of caused exceptions"
    (let [output (java.io.StringWriter.)
          cause (Exception. "Cause")
          exception (Exception. "Test Exception" cause)
          expected "java.lang.Exception: Test Exception
\tat my_code$outside.invoke(my_file.clj:321)
Caused by: java.lang.Exception: Cause
\tat my_code$start.invoke(my_file.clj:123)
\t... 4 stack levels elided ...
\tat my_code$end.invoke(my_file.clj:124)
"]
      (.setStackTrace exception
        (into-array
          [(StackTraceElement. "my_code$outside" "invoke" "my_file.clj" 321)]))
      (.setStackTrace cause
        (into-array [
          (StackTraceElement. "my_code$start" "invoke" "my_file.clj" 123)
          (StackTraceElement. "speclj.running$eval_characteristic" "invoke" "running.clj" 22)
          (StackTraceElement. "clojure.lang.RT" "load" "RT.java" 412)
          (StackTraceElement. "clojure.core$load$fn__4511" "invoke" "core.clj" 4905)
          (StackTraceElement. "clojure.core$load" "doInvoke" "core.clj" 4904)
          (StackTraceElement. "my_code$end" "invoke" "my_file.clj" 124)
          ]))
      (print-stack-trace exception output)
      (should= expected (.toString output))))
  )
)

(run-specs)
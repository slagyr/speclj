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

    (it "prints in yellow"
      (should= "\u001b[33mtext\u001b[0m" (yellow "text")))

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
\tat clojure_my_code.invoke(my_file.clj:666)
\tat speclj-my-code.invoke(my_file.clj:666)
\tat java_my_code.invoke(my_file.clj:666)
\t... 4 stack levels elided ...
\tat my_code$end.invoke(my_file.clj:124)
"]
      (.setStackTrace exception
        (into-array [
          (StackTraceElement. "my_code$start" "invoke" "my_file.clj" 123)
          (StackTraceElement. "clojure_my_code" "invoke" "my_file.clj" 666)
          (StackTraceElement. "speclj-my-code" "invoke" "my_file.clj" 666)
          (StackTraceElement. "java_my_code" "invoke" "my_file.clj" 666)
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

    (it "prefixes lines of text"
      (should= "--foo" (prefix "--" "foo"))
      (should= "++bar" (prefix "++" "bar"))
      (should= "=foobar" (prefix "=" "foo" "bar"))
      (should= "--foo\n--bar" (prefix "--" "foo\nbar")))

    (it "can indent"
      (should= "  foo" (indent 1 "foo"))
      (should= "   bar" (indent 1.5 "bar"))
      (should= "  foo\n  bar" (indent 1 "foo\nbar"))
      (should= "    foo\n    bar" (indent 2 "foo\nbar")))
  )
)

(run-specs)
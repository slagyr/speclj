(ns speclj.reporting-spec
  (:require [speclj.core #?(:cljs :refer-macros :default :refer) [describe context around it should=]]
            [speclj.config :as config]
            [speclj.run.standard :as standard]
            [speclj.reporting :as sut]))

(describe "Reporting"
  (context "without color"
    (around [it] (binding [config/*color?* false] (it)))

    (it "prints all colors as plain text"
      (should= "text" (sut/red "text"))
      (should= "text" (sut/green "text"))
      (should= "text" (sut/grey "text")))
    )

  (context "with color"
    (around [it] (binding [config/*color?* true] (it)))

    (it "prints in red"
      (should= "\u001b[31mtext\u001b[0m" (sut/red "text")))

    (it "prints in green"
      (should= "\u001b[32mtext\u001b[0m" (sut/green "text")))

    (it "prints in yellow"
      (should= "\u001b[33mtext\u001b[0m" (sut/yellow "text")))

    (it "prints in grey"
      (should= "\u001b[90mtext\u001b[0m" (sut/grey "text")))
    )

  (context "with elided stacktrace"
    (around [it] (binding [config/*full-stack-trace?* false] (it)))

    #?(:clj
       (it "prints elided stack traces"
         (let [exception (Exception. "Test Exception")
               expected  "java.lang.Exception: Test Exception
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
           (should= expected (sut/stack-trace-str exception)))))

    #?(:clj
       (it "prints elided stack traces of caused exceptions"
         (let [cause     (Exception. "Cause")
               exception (Exception. "Test Exception" cause)
               expected  "java.lang.Exception: Test Exception
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
           (should= expected (sut/stack-trace-str exception)))))

    (it "prefixes lines of text"
      (should= "--foo" (sut/prefix "--" "foo"))
      (should= "++bar" (sut/prefix "++" "bar"))
      (should= "=foobar" (sut/prefix "=" "foo" "bar"))
      (should= "--foo\n--bar" (sut/prefix "--" "foo\nbar")))

    (it "can indent"
      (should= "  foo" (sut/indent 1 "foo"))
      (should= "   bar" (sut/indent 1.5 "bar"))
      (should= "  foo\n  bar" (sut/indent 1 "foo\nbar"))
      (should= "    foo\n    bar" (sut/indent 2 "foo\nbar")))
    )
  )

(standard/run-specs)

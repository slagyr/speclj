(ns leiningen.spec
  (:use
    [clojure.java.io :only [file]])
  (:import
    [java.io BufferedInputStream]))

(defn- copy-bytes [in out]
  (with-open [input in]
    (loop [b (.read input)]
      (when-not (= -1 b)
        (.write out b)
        (recur (.read input))))))

(defn- java [jvm-args main-class args working-directory]
  (let [java-exe (str (System/getProperty "java.home") "/bin/java")
        command (into-array (concat [java-exe] jvm-args [main-class] args))
        process (.exec (Runtime/getRuntime) command nil (file working-directory))
        output (BufferedInputStream. (.getInputStream process))
        output-thread (Thread. #(copy-bytes output System/out))
        error (BufferedInputStream. (.getErrorStream process))
        error-thread (Thread. #(copy-bytes error System/err))]
    (.start output-thread)
    (.start error-thread)
    (.waitFor process)
    (.join output-thread 1000)
    (.join error-thread 1000)
    (.exitValue process)))

(defn- compute-classpath-string [project]
  (clojure.string/join java.io.File/pathSeparatorChar
        ((or (ns-resolve (the-ns 'leiningen.classpath) 'get-classpath)
             (ns-resolve (the-ns 'leiningen.core.classpath) 'get-classpath))
          project)))

(defn- prepare [project]
  (try
    (require 'leiningen.core.eval)
    ((ns-resolve 'leiningen.core.eval 'prep) project)
    (catch java.io.FileNotFoundException e
      (require 'leiningen.classpath
               'leiningen.compile)
      ((ns-resolve 'leiningen.compile 'prep) project false))))

(defn spec
  "Speclj - pronounced \"speckle\": a TDD/BDD framework for Clojure.

You're currently using Speclj's Leiningen plugin.  To get the Speclj's help
documentation, as opposed to this message provided by Leinigen, try this:

  lein spec --speclj

That ough to do the trick."
  [project & args]
  (prepare project)
  (let [speclj-args (cons "-c" args)
        classpath (compute-classpath-string project)
        jvm-args ["-cp" classpath "-Dspeclj.invocation=lein spec"]]
    (try
      (require 'leiningen.core.main)
      ((ns-resolve (the-ns 'leiningen.core.main) 'exit) (java jvm-args "speclj.main" speclj-args (:root project)))
      (catch java.io.FileNotFoundException e
        (java jvm-args "speclj.main" speclj-args (:root project))))))

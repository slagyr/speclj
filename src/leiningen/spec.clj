(ns leiningen.spec
  (:use
    [clojure.java.io :only [file]]
    [leiningen.classpath :only [get-classpath-string]])
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

(defn spec [project & args]
  (let [speclj-args (cons "-c" args)
        classpath (get-classpath-string project)
        jvm-args ["-cp" classpath "-Dspeclj.invocation=lein spec"]]
    (java jvm-args "speclj.main" speclj-args (:root project))))

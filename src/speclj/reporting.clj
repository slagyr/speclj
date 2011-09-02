(ns speclj.reporting
  (:use
    [speclj.exec :only (pass? fail?)]
    [speclj.config :only (*reporter* *color?* *full-stack-trace?*)]
    [clojure.string :as string :only (split join)])
  (:import
    [speclj.exec PassResult FailResult PendingResult]
    [java.io PrintWriter StringWriter]))

(defn- classname->filename [classname]
  (let [root-name (first (split classname #"\$"))]
    (str
      (string/replace root-name "." (System/getProperty "file.separator"))
      ".clj")))

(defn failure-source [exception]
  (let [source (nth (.getStackTrace exception) 0)
        classname (.getClassName source)
        filename (classname->filename classname)]
    (if-let [url (.getResource (clojure.lang.RT/baseLoader) filename)]
      (str (.getFile url) ":" (.getLineNumber source))
      (str filename ":" (.getLineNumber source)))))

(defn tally-time [results]
  (loop [tally 0.0 results results]
    (if (seq results)
      (recur (+ tally (.seconds (first results))) (rest results))
      tally)))

(defprotocol Reporter
  (report-message [reporter message])
  (report-description [this description])
  (report-pass [this result])
  (report-pending [this result])
  (report-fail [this result])
  (report-runs [this results]))

(defmulti report-run (fn [result reporter] (type result)))
(defmethod report-run PassResult [result reporter]
  (report-pass reporter result))
(defmethod report-run FailResult [result reporter]
  (report-fail reporter result))
(defmethod report-run PendingResult [result reporter]
  (report-pending reporter result))

(defn- stylizer [code]
  (fn [text]
    (if *color?*
      (str "\u001b[" code "m" text "\u001b[0m")
      text)))

(def red (stylizer "31"))
(def green (stylizer "32"))
(def yellow (stylizer "33"))
(def grey (stylizer "90"))

(defn- elide-level? [stack-element]
  (let [classname (.getClassName stack-element)]
    (or
      (.startsWith classname "clojure.")
      (.startsWith classname "speclj.")
      (.startsWith classname "java."))))


(defn- print-elides [n]
  (if (< 0 n)
    (println "\t..." n "stack levels elided ...")))

(declare print-exception)

(defn- print-stack-levels [exception]
  (loop [levels (seq (.getStackTrace exception)) elides 0]
    (if (seq levels)
      (let [level (first levels)]
        (if (elide-level? level)
          (recur (rest levels) (inc elides))
          (do
            (print-elides elides)
            (println "\tat" (.toString level))
            (recur (rest levels) 0))))
      (print-elides elides)))
  (if-let [cause (.getCause exception)]
    (print-exception "Caused by:" cause)))

(defn- print-exception [prefix exception]
  (if prefix
    (println prefix (.toString exception))
    (println (.toString exception)))
  (print-stack-levels exception))

(defn stack-trace [exception]
  (let [output (StringWriter.)]
    (binding [*out* (PrintWriter. output)]
      (if *full-stack-trace?*
        (.printStackTrace exception *out*)
        (print-exception nil exception)))
    (.toString output)))

(defn print-stack-trace [exception writer]
  (if *full-stack-trace?*
    (.printStackTrace exception (PrintWriter. writer))
    (binding [*out* writer]
      (print-exception nil exception))))

(defn prefix [pre & args]
  (let [value (apply str args)
        lines (split value #"[\r\n]+")
        prefixed-lines (map #(str pre %) lines)]
    (join (System/getProperty "line.separator") prefixed-lines)))

(defn indent [n & args]
  (let [spaces (int (* n 2.0))
        indention (reduce (fn [p _] (str " " p)) "" (range spaces))]
    (apply prefix indention args)))


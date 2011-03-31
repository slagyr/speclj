(ns speclj.reporting
  (:use
    [speclj.exec :only (pass? fail?)]
    [speclj.config :only (*reporter* *color?* *full-stack-trace?*)]))

(defn failure-source [exception]
  (let [source (nth (.getStackTrace exception) 0)]
    (if-let [filename (.getFileName source)]
      (str (.getCanonicalPath (java.io.File. filename)) ":" (.getLineNumber source))
      "Unknown source")))

(defn tally-time [results]
  (loop [tally 0.0 results results]
    (if (seq results)
      (recur (+ tally (.seconds (first results))) (rest results))
      tally)))

(defprotocol Reporter
  (report-message [reporter message])
  (report-description [this description])
  (report-pass [this result])
  (report-fail [this result])
  (report-runs [this results]))

(defn- stylizer [code]
  (fn [text]
    (if *color?*
      (str "\u001b[" code "m" text "\u001b[0m")
      text)))

(def red (stylizer "31"))
(def green (stylizer "32"))
(def grey (stylizer "90"))

(defn- elide-level? [stack-element]
  (let [classname (.getClassName stack-element)]
    (or
      (.startsWith classname "clojure")
      (.startsWith classname "speclj")
      (.startsWith classname "java"))))


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

(defn print-stack-trace [exception writer]
  (if *full-stack-trace?*
    (.printStackTrace exception (java.io.PrintWriter. writer))
    (binding [*out* writer]
      (print-exception nil exception))))


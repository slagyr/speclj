(ns speclj.util)

(def endl (System/getProperty "line.separator"))

(def seconds-format (java.text.DecimalFormat. "0.00000"))

(defn secs-since [start]
  (/ (double (- (System/nanoTime) start)) 1000000000.0))

(defn str-time-since [start]
  (str (.format seconds-format (secs-since start)) " seconds"))
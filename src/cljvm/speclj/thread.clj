(ns speclj.thread)

(def pool (atom #{}))

(defn join [thread] (.join thread))
(defn start [thread] (.start thread))
(defn interrupt [thread] (.interrupt thread))
(defn alive? [thread] (.isAlive thread))
(defn sleep [millis] (Thread/sleep millis))
(defn join-all []
  (doseq [thread @pool]
    (join thread)
    (swap! pool disj thread)))

(defn ->Thread [runnable] (Thread. ^Runnable runnable))
(defmacro create [& body] `(->Thread (fn [] ~@body)))

(defmacro spawn [& body]
  `(let [thread# (create ~@body)]
     (swap! pool conj thread#)
     (start thread#)
     thread#))

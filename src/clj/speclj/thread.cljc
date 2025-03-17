(ns speclj.thread)

(def pool (atom #{}))
(defn join [thread] (#?(:clj .join :cljr .Join) thread))
(defn start [thread] (#?(:clj .start :cljr .Start) thread))
(defn interrupt [thread] (#?(:clj .interrupt :cljr .Interrupt) thread))
(defn alive? [thread] (#?(:clj .isAlive :cljr .-IsAlive) thread))
(defn sleep [millis] (#?(:clj Thread/sleep :cljr System.Threading.Thread/Sleep) millis))

(defn join-all []
  (doseq [thread @pool]
    (join thread)
    (swap! pool disj thread)))

#?(:clj (defn ->Thread [runnable] (Thread. ^Runnable runnable)))

(defmacro create [& body]
  #?(:clj  `(->Thread (fn [] ~@body))
     :cljr `(System.Threading.Thread.
             (gen-delegate System.Threading.ThreadStart [] ~@body))))

(defmacro spawn [& body]
  `(let [thread# (create ~@body)]
     (swap! pool conj thread#)
     (start thread#)
     thread#))

(ns speclj.async-spec
  (:require [speclj.core #+clj :refer #+cljs :refer-macros [describe it should= with around -fail]]))

(defn now []
  #+clj (.getTime (java.util.Date.))
  #+cljs (.getTime (js/Date.)))

(defn timeout [f millis]
  #+clj (future (Thread/sleep millis) (f))
  #+cljs (js/setTimeout f millis))

(def register (atom nil))

(def ^:dynamic *done* nil)

(defn done []
  (if *done*
    (*done*)
    (throw (ex-info "(done) called outside async environment" {}))))

(describe "Async"

  (around [it]
          (let [start (now)
                done-flag (atom false)
                done-fn #(reset! done-flag true)]
            (println "start*: " start)
            (letfn [(check-fn []
                              (let [time (now)
                                    timed-out? (> (- time start) 1000)]
                                (println "checking " (- time start))
                                (if timed-out?
                                  (do
                                    (println "timeout")
                                    (-fail (str "timeout " (- time start))))
                                  (timeout check-fn 10))))]
              (binding [*done* done-fn]
                (try
                  (it)
                  (catch
                    #+clj Throwable
                    #+cljs :default e
                    ;(done)
                    (println e)
                    (throw e)))
                (check-fn)
                ))))

  (it "sleeper"
    (let [start (now)]
      (println "start: " start)
      (timeout
        #(let [end (now)]
          (println "end, slept: " end (- end start))
          (reset! register end)
          (should= true (>= end start))
          ;(done)
          )
        5000))
    (should= nil @register))

  )


(ns specjs.async
  (:require [speclj.results :as results]))

; Abondining this feature for now
; Notes
;   - Exceptions thrown in async functions are not caught by test. See: https://gist.github.com/slagyr/42c06f51d9faafe2fc58
;     - The use of exceptions for failure handling may not work
;   - Running a characterstic requires a callback function to know when the spec has completed.
;     - The flow of control during test run will likely need to be reversed

(def ^:dynamic *async?* false)

(def ^:dynamic *done* nil)

(defn done []
  (if *done*
    (*done*)
    (throw (ex-info "(done) called outside async environment" {}))))

(defn now []
  #+clj (.getTime (java.util.Date.))
  #+cljs (.getTime (js/Date.)))

(defn timeout [f millis]
  #+clj (future (Thread/sleep millis) (f))
  #+cljs (js/setTimeout f millis))

(defn do-characteristic [body callback]
  (let [start (now)
        done-fn (callback results/pass-result nil)]
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
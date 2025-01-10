(ns speclj.interval
  (:require [speclj.platform :as platform]
            [speclj.thread :as thread]))

(def ^:private intervals (atom {}))

(defn- ->interval [millis handler]
  {:key      (gensym)
   :delay-ms millis
   :handler  handler
   :cleared? (atom false)})

(defn- loop-for-interval [last-run {:keys [delay-ms handler cleared?] :as interval}]
  (let [run-at    (+ last-run delay-ms)
        sleep-for (- run-at (platform/current-millis))]
    (when (pos? sleep-for) (thread/sleep sleep-for))
    (when-not @cleared? (handler))
    (when-not @cleared? (recur (platform/current-millis) interval))))

(defn set-interval [millis handler]
  (let [interval (->interval millis handler)
        thread   (thread/spawn (loop-for-interval 0 interval))
        interval (assoc interval :thread thread)]
    (swap! intervals assoc (:key interval) interval)
    (:key interval)))

(defn clear-interval [key]
  (when-let [interval (get @intervals key)]
    (reset! (:cleared? interval) true)
    (thread/join (:thread interval))
    (swap! intervals dissoc key)
    key))

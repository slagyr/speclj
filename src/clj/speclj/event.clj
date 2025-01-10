(ns speclj.event
  (:require [speclj.platform :as platform]
            [speclj.thread :as thread]))

(def new-line 10)
(defn new-line? [char] (= new-line char))

(def ^:private enter-handlers (atom #{}))
(def ^:private read-thread (atom nil))

(defn- watch-for-enter-key []
  (while true
    (when (new-line? (platform/read-in))
      (doseq [handler @enter-handlers]
        (handler)))))

(defn add-enter-listener [handler]
  (when-not (contains? @enter-handlers handler)
    (swap! enter-handlers conj handler)
    (when-not @read-thread
      (reset! read-thread (thread/spawn (watch-for-enter-key))))))

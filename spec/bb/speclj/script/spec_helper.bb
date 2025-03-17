(ns speclj.script.spec-helper
  (:require [clojure.java.shell :as shell]
            [speclj.core :refer :all]
            [speclj.script.file :as file]
            [speclj.script.core :as script]
            [speclj.script.file.memory :as memory-file]))

(defn with-memory-files []
  (list
    (before (reset! file/impl :memory))
    (after (reset! file/impl nil)
           (memory-file/clear!))))

(defn stub-system-exit []
  (redefs-around [script/system-exit (stub :script/system-exit)]))

(defn stub-shell []
  (redefs-around [shell/sh (stub :shell/sh {:return {:exit 0}})]))

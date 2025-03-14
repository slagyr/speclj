(ns speclj.script.executable-spec
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [speclj.core :refer :all]))

(defn first-line [path]
  (with-open [reader (io/reader path)]
    (first (line-seq reader))))

(defmacro test-script-is-executable [filename]
  `(let [filename# ~filename]
     (it filename#
       (let [path# (str "dev/speclj/script/" filename#)]
         (should-be fs/exists? path#)
         (should-be fs/executable? path#)
         (should= "#!/usr/bin/env bb" (first-line path#))))))

(defmacro should-contain-shell-task [tasks task-key path]
  `(let [tasks#    ~tasks
         task-key# ~task-key]
     (should-contain task-key# tasks#)
     (should= (list (symbol "shell") ~path) (:task (tasks# task-key#)))))

(describe "Executables"

  (it "has bb.edn tasks configured"
    (let [{:keys [tasks]} (edn/read-string (slurp "bb.edn"))]
      (should= 3 (count tasks))
      (should-contain 'spec tasks)
      (should-contain-shell-task tasks 'install-clj "dev/speclj/script/install_clj.bb")
      (should-contain-shell-task tasks 'install-cljr "dev/speclj/script/install_cljr.bb")))

  (context "script structure"
    (test-script-is-executable "install_clj.bb")
    (test-script-is-executable "install_cljr.bb")
    )
)
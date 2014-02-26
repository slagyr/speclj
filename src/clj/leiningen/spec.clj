(ns leiningen.spec
  (:require [leiningen.core.eval :refer [eval-in-project]]))

(defn- exit-if-needed [exit-code]
  (cond
    (nil? exit-code) 0
    (not (number? exit-code)) (println *err* (str "Unusual exit code: " exit-code))
    (not (zero? exit-code))
    (try
      (require 'leiningen.core.main)
      ((ns-resolve (the-ns 'leiningen.core.main) 'exit) exit-code)
      (catch java.io.FileNotFoundException e))))

(defn- with-paths [args project]
  (if (some #(not (.startsWith % "-")) args)
    args
    (concat args (:test-paths project))))

(defn- build-args [project args]
  (-> args
    seq
    (conj "-c")
    (with-paths project)
    vec))

(defn spec
  "Speclj - pronounced \"speckle\": a TDD/BDD framework for Clojure.

You're currently using Speclj's Leiningen plugin.  To get the Speclj's help
documentation, as opposed to this message provided by Leiningen, try this:

  lein spec --speclj

That ought to do the trick."
  [project & args]
  (let [project (assoc project :eval-in (get project :speclj-eval-in :subprocess))
        speclj-args (build-args project args)]
    (exit-if-needed
      (eval-in-project project
        `(apply speclj.cli/run ~speclj-args)
        '(require 'speclj.cli))
      )))

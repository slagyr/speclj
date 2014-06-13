(ns leiningen.spec
  (:require [leiningen.core.eval :refer [eval-in-project]]
            [leiningen.core.main :as main]))

(defn make-run-form [project speclj-args]
  (let [exit-fn (if (or (:eval-in-leiningen project)
                        (= (:eval-in project) :leiningen))
                  `main/exit
                  `System/exit)]
    `(let [failures# (speclj.cli/run ~@speclj-args)]
       (~exit-fn (min 255 failures#)))))

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
        speclj-args (build-args project args)
        run-form (make-run-form project speclj-args)
        init-form '(require 'speclj.cli)]
      (eval-in-project project run-form init-form)))
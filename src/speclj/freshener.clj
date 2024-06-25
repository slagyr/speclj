(ns speclj.freshener
  (:use
    [clojure.java.io :only (file)])
  (:require
    [clojure.tools.namespace.repl :as repl]
    [clojure.tools.namespace.reload :as reload]))

(defn find-files-in
  "Returns a seq of all files (matching the regex) contained in the given directories."
  [pattern & dirs]
  (let [dirs (map #(.getCanonicalFile %) dirs)
        files (reduce #(into %1 (file-seq (file %2))) [] dirs)
        files (remove #(.isHidden %) files)
        clj-files (filter #(re-matches pattern (.getName %)) files)]
    clj-files))

(def clj-file-regex #".*\.clj(c)?")
(defn clj-files-in
  "Returns a seq of all clojure source files contained in the given directories."
  [& dirs] (apply find-files-in clj-file-regex dirs))

(defn return-n [n]
  n)

(defn freshen []
  (repl/scan)
  (alter-var-root #'repl/refresh-tracker reload/track-reload)
  (apply repl/set-refresh-dirs []))

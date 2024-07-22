(ns speclj.freshener
  (:use
    [clojure.java.io :only (file)])
  (:require
    [clojure.tools.namespace.dir :as dir]
    [clojure.tools.namespace.repl :as repl]
    [clojure.tools.namespace.track :as track]
    [clojure.tools.namespace.reload :as reload]
    [clojure.tools.namespace.file :as file]))

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

(defn remove-value [val coll]
  (remove #(= % val) coll))

(defn remove-ignore [tracker namespace]
  (when-let [file (first (some #(when (= (val %) namespace) %) (::file/filemap tracker)))]
    (alter-var-root #'repl/refresh-tracker
                    (constantly
                      (assoc tracker
                        ::track/load (remove-value namespace (::track/load tracker))
                        ::track/unload (remove-value namespace (::track/unload tracker))
                        ::file/filemap (dissoc (::file/filemap tracker) file)
                        ::dir/files (set (remove-value file (::dir/files tracker))))))))

(def ignored-namespaces ['speclj.config 'speclj.run.vigilant
                         'speclj.results 'speclj.core
                         'speclj.reporting 'speclj.running])

(defn freshen []
  (repl/scan)
  (doseq [namespace ignored-namespaces]
    (remove-ignore repl/refresh-tracker namespace)
    )
  (alter-var-root #'repl/refresh-tracker reload/track-reload)
  (repl/set-refresh-dirs))

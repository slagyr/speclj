(ns speclj.freshener
  (:require [clojure.set :as set]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.file :as file]
            [clojure.tools.namespace.reload :as reload]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.track :as track]
            [speclj.io :as io]
            [speclj.platform :as platform]
            [speclj.reporting]))

(defn- files-in [dir]
  (-> dir io/canonical-file io/as-file file-seq))

(defn find-files-in
  "Returns a seq of all files (matching the regex) contained in the given directories."
  [pattern & dirs]
  (->> (mapcat files-in dirs)
       (remove io/hidden?)
       (filter #(re-matches pattern (io/file-name %)))))

(defn clj-files-in
  "Returns a seq of all clojure source files contained in the given directories."
  [& dirs]
  (apply find-files-in platform/source-file-regex dirs))

(defn find-key-by-value [m val]
  (some (fn [[k v]] (when (= v val) k)) m))

(defn- find-reloaded-files [tracker]
  (for [ns (::track/load tracker)]
    (find-key-by-value (::file/filemap tracker) ns)))

(defn- ignore-namespaces [tracker namespaces]
  (let [filemap (::file/filemap tracker)
        files   (set (keep (partial find-key-by-value filemap) namespaces))]
    (assoc tracker
      ::track/load (remove namespaces (::track/load tracker))
      ::track/unload (remove namespaces (::track/unload tracker))
      ::file/filemap (apply dissoc filemap files)
      ::dir/files (set/difference (::dir/files tracker) files))))

(def ignored-namespaces
  #{'speclj.config
    'speclj.run.vigilant
    'speclj.results
    'speclj.core
    'speclj.reporting
    'speclj.running})

(defn- ignore-speclj [tracker] (ignore-namespaces tracker ignored-namespaces))

(defn freshen []
  (repl/scan {:platform platform/find-platform})
  (alter-var-root #'repl/refresh-tracker ignore-speclj)
  (let [reloaded-files (find-reloaded-files repl/refresh-tracker)]
    (alter-var-root #'repl/refresh-tracker reload/track-reload)
    (repl/set-refresh-dirs)
    reloaded-files))

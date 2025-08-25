(ns speclj.freshener
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.file :as file]
            [clojure.tools.namespace.parse :as parse]
            [clojure.tools.namespace.reload :as reload]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.track :as track]
            [speclj.io :as io]
            [speclj.platform :as platform]
            [speclj.reporting]))

(def ignored-namespaces
  #{'speclj.config
    'speclj.run.vigilant
    'speclj.results
    'speclj.core
    'speclj.components
    'speclj.reporting
    'speclj.report.progress
    'speclj.report.silent
    'speclj.running})

(defn- files-in [dir]
  (-> dir io/canonical-file io/as-file file-seq))

(defn find-files-in
  "Returns a seq of all files (matching the regex) contained in the given directories."
  [pattern & dirs]
  (->> (mapcat files-in dirs)
       (remove io/hidden?)
       (filter #(re-matches pattern (io/file-name %)))))

(defn- file-ns-name [platform file]
  (-> file
      (file/read-file-ns-decl (:read-opts platform))
      parse/name-from-ns-decl))

(defn- prefer-platform-extension [platform files]
  (first
    (for [extension (:extensions platform)
          file      files
          :when (str/ends-with? (str file) extension)]
      file)))

(defn- extensions->pattern [extensions]
  (let [re-extensions (->> extensions
                           (map #(str/escape % {\. "\\."}))
                           (str/join "|"))]
    (re-pattern (str ".*(" re-extensions ")$"))))

(defn clj-files-in
  "Returns a seq of all clojure source files contained in the given directories for the platform."
  [dirs platform]
  (let [regex (-> platform :extensions extensions->pattern)]
    (->> (map io/as-file dirs)
         (apply find-files-in regex)
         (group-by (partial file-ns-name platform))
         (remove (comp ignored-namespaces first))
         (map #(prefer-platform-extension platform (second %))))))

(defn load-clj-files-in [dirs]
  (->> (clj-files-in dirs platform/find-platform)
       (run! (comp platform/load-file str))))

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

(defn- ignore-speclj [tracker] (ignore-namespaces tracker ignored-namespaces))

(defn make-fresh! [tracker-var]
  (alter-var-root tracker-var ignore-speclj)
  (let [reloaded-files (find-reloaded-files @tracker-var)]
    (alter-var-root tracker-var reload/track-reload)
    reloaded-files))

(defn freshen []
  (repl/scan {:platform platform/find-platform})
  (make-fresh! #'repl/refresh-tracker))

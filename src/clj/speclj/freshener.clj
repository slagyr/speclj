(ns speclj.freshener
  (:require
    [clojure.tools.namespace.dir :as dir]
    [clojure.tools.namespace.file :as file]
    [clojure.tools.namespace.reload :as reload]
    [clojure.tools.namespace.repl :as repl]
    [clojure.tools.namespace.track :as track]
    [speclj.config]
    [speclj.io :as io]
    [speclj.reporting]))

(defn find-files-in
  "Returns a seq of all files (matching the regex) contained in the given directories."
  [pattern & dirs]
  (->> (map io/canonical-file dirs)
       (reduce #(into %1 (file-seq (io/as-file %2))) [])
       (remove io/hidden?)
       (filter #(re-matches pattern (io/file-name %)))))

(def clj-file-regex #".*\.clj(c)?")
(defn clj-files-in
  "Returns a seq of all clojure source files contained in the given directories."
  [& dirs]
  (apply find-files-in clj-file-regex dirs))

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

(defn find-key-by-value [m val]
  (some (fn [[k v]] (when (= v val) k)) m))

(def ignored-namespaces ['speclj.config 'speclj.run.vigilant
                         'speclj.results 'speclj.core
                         'speclj.reporting 'speclj.running])

(defn freshen []
  (repl/scan)
  (doseq [namespace ignored-namespaces]
    (remove-ignore repl/refresh-tracker namespace))
  (let [reloaded-files
        (for [ns (::track/load repl/refresh-tracker)]
          (find-key-by-value (::file/filemap repl/refresh-tracker) ns))]
    (alter-var-root #'repl/refresh-tracker reload/track-reload)
    (repl/set-refresh-dirs)
    reloaded-files))

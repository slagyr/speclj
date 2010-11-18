(ns speclj.runner.vigilant-runner
  (:use
    [speclj.running :only (do-description report *runner*)]
    [speclj.util]
    [speclj.reporting :only (report-runs active-reporter)]
    [clojure.set :only (difference union)])
  (:import
    [speclj.running Runner]
    [java.io PushbackReader FileReader File]
    [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

; Data types -----------------------------------------------------------------------------------------------------------

(deftype VigilantRunner [listing dir results]
  Runner
  (run [this description reporter]
    (let [run-results (do-description description reporter)]
      (swap! results into run-results)))
  (report [this reporter]
    (report-runs reporter @results))

  Object
  (toString [this] (str "on " dir endl "listing: " (apply str (interleave (repeat endl) @listing)))))

(defn new-vigilant-runner [dir]
  (VigilantRunner. (atom {}) dir (atom [])))

(deftype FileTracker [ns mod-time dependencies]
  Object
  (toString [this] (str "ns: " ns " mod-time: " mod-time " dependencies: " dependencies)))

(defn new-file-tracker
  [ns mod-time dependencies]
  (FileTracker. ns mod-time dependencies))

; Resolving ns names ---------------------------------------------------------------------------------------------------

(defn ns-to-filename [ns]
  (str (apply str (replace {\. \/ \- \_} (name ns))) ".clj"))

(defn ns-to-file [ns]
  (let [relative-filename (ns-to-filename ns)
        url (.getResource (.getContextClassLoader (Thread/currentThread)) relative-filename)]
    (if (= "file" (.getProtocol url))
      (File. (.getFile url))
      nil)))

(defn- ns-form? [form]
  (and (list? form) (= 'ns (first form))))

(defn read-ns-form [file]
  (try
    (let [reader (PushbackReader. (FileReader. file))]
      (try
        (loop [form (read reader)]
          (if (ns-form? form)
            form
            (recur (read reader))))
        (finally (.close reader))))
    (catch Exception e nil)))

; Parsing the ns form --------------------------------------------------------------------------------------------------

(defn- compose-ns [prefix lib]
  (if prefix
    (symbol (str prefix \. lib))
    lib))

(defn- ns-for-part [prefix arg]
  (cond
    (symbol? arg) (compose-ns prefix arg)
    (and (vector? arg) (or (nil? (second arg)) (keyword? (second arg)))) (compose-ns prefix (first arg))
    :else (map #(ns-for-part (compose-ns prefix (first arg)) %) (rest arg))))

(defn- dependencies-in-ns-part [args]
  (map #(ns-for-part nil %) (rest args)))

(defn dependencies-in-ns [ns-form]
  (let [dependency-parts (filter #(and (list? %) (#{:use :require} (first %))) ns-form)
        ns-list (map #(dependencies-in-ns-part %) dependency-parts)]
    (set (flatten ns-list))))

; File tracking --------------------------------------------------------------------------------------------------------

(defn- modified? [file tracker]
  (> (.lastModified file) (.mod-time tracker)))

(declare update-tracking-for-files)
(defn- update-tracking-for-file [listing file batch]
  (let [tracker (get listing key)
        ns-form (read-ns-form file)
        no-update-required (not (or (nil? tracker) (modified? file tracker)))]
    (if (or no-update-required (nil? ns-form))
      [listing batch]
      (let [dependency-names (dependencies-in-ns ns-form)
            dependencies (vec (filter (complement nil?) (map #(ns-to-file %) dependency-names)))
            [listing batch] (update-tracking-for-files listing dependencies batch)
            file-ns (second ns-form)
            updated-tracker (new-file-tracker file-ns (.lastModified file) dependencies)]
        [(assoc listing file updated-tracker) batch]))))

(defn- update-tracking-for-files
  ([listing files] ((update-tracking-for-files listing files #{}) 0))
  ([listing files batch]
    (loop [[listing batch] [listing batch] files files]
      (if (not (seq files))
        [listing batch]
        (let [file (first files)]
          (if (contains? batch file)
            (recur [listing batch] (rest files))
            (recur (update-tracking-for-file listing file (conj batch file)) (rest files))))))))

(defn track-files [runner & files]
  (swap! (.listing runner) #(update-tracking-for-files % files)))

(def clj-file-regex #".*\.clj")
(defn- clj-files [runner]
  (let [all-files (file-seq (.dir runner))]
    (filter #(re-matches clj-file-regex (.getName %)) all-files)))

(defn- depends-on? [dependency listing dependent]
  (some (partial = dependency) (.dependencies (get listing dependent))))

(defn- has-dependent? [listing file]
  (some #(depends-on? file listing %) (keys listing)))

(defn- clean-deleted-files
  ([listing] (clean-deleted-files listing (filter #(not (.exists %)) (keys listing))))
  ([listing files-to-delete]
    (if (not (seq files-to-delete))
      listing
      (let [dependencies (reduce #(into %1 (.dependencies (get listing %2))) [] files-to-delete)
            listing (apply dissoc listing files-to-delete)
            unused-dependencies (filter #(not (has-dependent? listing %)) dependencies)]
        (clean-deleted-files listing unused-dependencies)))))

(defn updated-files [runner]
  (swap! (.listing runner) clean-deleted-files)
  (let [observed-files (set (clj-files runner))
        listing @(.listing runner)
        tracked-files (set (keys listing))
        new-files (difference observed-files tracked-files)
        modified-files (filter #(modified? % (get listing %)) tracked-files)]
    (concat new-files modified-files)))

(defn reload-files [runner & files]
  (let [listing @(.listing runner)
        trackers (vec (map listing files))
        nses (vec (map #(.ns %) trackers))]
    (if (seq nses)
      (do
        (doseq [ns nses] (remove-ns ns))
        (dosync (alter @#'clojure.core/*loaded-libs* difference (set nses)))
        (apply require nses)))))

(defn dependents-of [listing file]
  (vec (filter #(depends-on? file listing %) (keys listing))))


; Main running ---------------------------------------------------------------------------------------------------------

(defn- tick [runner]
  (binding [*runner* runner]
    (if-let [updates (seq (updated-files runner))]
      (try
        (apply track-files runner updates)
        (let [listing @(.listing runner)
              files-to-reload (reduce #(into %1 (dependents-of listing %2)) updates updates)]
          (println "files-to-reload: " files-to-reload)
          (apply reload-files runner files-to-reload))
        (report runner (active-reporter))
        (catch Exception e (.printStackTrace e))))
    (swap! (.results runner) (fn [_] []))))

(defn watch [dirname]
  (println "watching:" dirname)
  (let [runner (new-vigilant-runner (File. dirname))
        scheduler (ScheduledThreadPoolExecutor. 1)
        runnable (fn [] (tick runner))]
    (.scheduleWithFixedDelay scheduler runnable 0 1 TimeUnit/SECONDS)))


(ns speclj.run.vigilant
  (:use
    [speclj.running :only (do-description run-and-report run-description clj-files-in)]
    [speclj.util]
    [speclj.reporting :only (report-runs report-message print-stack-trace)]
    [clojure.set :only (difference union)]
    [speclj.config :only (active-runner active-reporter config-bindings *specs*)])
  (:import
    [speclj.running Runner]
    [java.io PushbackReader FileReader File]
    [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

; Resolving ns names ---------------------------------------------------------------------------------------------------

(defn ns-to-filename [ns]
  (str (apply str (replace {\. \/ \- \_} (name ns))) ".clj"))

(defn ns-to-file [ns]
  (let [relative-filename (ns-to-filename ns)
        url (.getResource (.getContextClassLoader (Thread/currentThread)) relative-filename)]
    (if (and url (= "file" (.getProtocol url)))
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

(defn- depending-names-of-part [args]
  (map #(ns-for-part nil %) (filter (complement keyword?) (rest args))))

(defn depending-names-of [ns-form]
  (let [dependency-parts (filter #(and (list? %) (#{:use :require} (first %))) ns-form)
        ns-list (map #(depending-names-of-part %) dependency-parts)]
    (set (flatten ns-list))))

(defn- depending-files-of [ns-form]
  (if ns-form
    (let [dependency-names (depending-names-of ns-form)
          dependency-filenames (map #(ns-to-file %) dependency-names)]
      (vec (filter identity dependency-filenames)))
    []))

(defn- ns-of [ns-form]
  (if ns-form
    (second ns-form)
    nil))

; File tracking --------------------------------------------------------------------------------------------------------

(deftype FileTracker [ns mod-time dependencies]
  Object
  (toString [this] (str "ns: " ns " mod-time: " mod-time " dependencies: " dependencies)))

(defn new-file-tracker [ns mod-time dependencies]
  (FileTracker. ns mod-time dependencies))

(defn- modified? [file tracker]
  (> (.lastModified file) (.mod-time tracker)))

(declare update-tracking-for-files)
(defn- update-tracking-for-file [listing file batch]
  (let [tracker (get listing key)
        ns-form (read-ns-form file)
        no-update-required (not (or (nil? tracker) (modified? file tracker)))]
    (if no-update-required
      [listing batch]
      (let [dependencies (depending-files-of ns-form)
            [listing batch] (update-tracking-for-files listing dependencies batch)
            ns (ns-of ns-form)
            updated-tracker (new-file-tracker ns (.lastModified file) dependencies)]
        [(assoc listing file updated-tracker) batch]))))

(defn- update-tracking-for-files
  ([listing files] (nth (update-tracking-for-files listing files #{}) 0))
  ([listing files batch]
    (loop [[listing batch] [listing batch] files files]
      (if (not (seq files))
        [listing batch]
        (let [file (first files)]
          (if (contains? batch file)
            (recur [listing batch] (rest files))
            (recur (update-tracking-for-file listing file (conj batch file)) (rest files))))))))

; Tracker Dependencies -------------------------------------------------------------------------------------------------

(defn- depends-on? [dependency listing dependent]
  (some (partial = dependency) (.dependencies (get listing dependent))))

(defn- has-dependent? [listing file]
  (some #(depends-on? file listing %) (keys listing)))

(defn- with-dependency [new-dependents dependents file tracker]
  (if (some dependents (.dependencies tracker))
    (conj new-dependents file)
    new-dependents))

(defn dependents-of
  ([listing files] (dependents-of listing (set files) #{}))
  ([listing files dependents]
    (loop [files files dependents dependents]
      (let [new-dependents (reduce (fn [new-dependents [file tracker]] (with-dependency new-dependents files file tracker)) #{} listing)]
        (if (seq new-dependents)
          (recur new-dependents (into dependents new-dependents))
          dependents)))))


; High level -----------------------------------------------------------------------------------------------------------

(defn track-files [runner & files]
  (swap! (.listing runner) #(update-tracking-for-files % files)))

(defn updated-files [runner directories]
  (let [observed-files (set (apply clj-files-in directories))
        listing @(.listing runner)
        tracked-files (set (keys listing))
        new-files (difference observed-files tracked-files)
        modified-files (filter #(modified? % (get listing %)) tracked-files)]
    (concat new-files modified-files)))

(defn clean-deleted-files
  ([runner] (swap! (.listing runner)
    (fn [listing] (clean-deleted-files listing (filter #(not (.exists %)) (keys listing))))))
  ([listing files-to-delete]
    (if (not (seq files-to-delete))
      listing
      (let [dependencies (reduce #(into %1 (.dependencies (get listing %2))) [] files-to-delete)
            listing (apply dissoc listing files-to-delete)
            unused-dependencies (filter #(not (has-dependent? listing %)) dependencies)]
        (clean-deleted-files listing unused-dependencies)))))

(defn reload-files [runner & files]
  (let [listing @(.listing runner)
        trackers (vec (filter identity (map listing files)))
        nses (vec (filter identity (map #(.ns %) trackers)))]
    (if (seq nses)
      (do
        (doseq [ns nses] (remove-ns ns))
        (dosync (alter @#'clojure.core/*loaded-libs* difference (set nses)))
        (apply require nses)))))

; Main -----------------------------------------------------------------------------------------------------------------

(defn- tick [configuration]
  (with-bindings configuration
    (let [runner (active-runner)
          reporter (active-reporter)
          start-time (System/nanoTime)]
      (clean-deleted-files runner)
      (if-let [updates (seq (updated-files runner *specs*))]
        (try
          (report-message reporter (str endl "----- " (str (java.util.Date.) " -------------------------------------------------------------------")))
          (apply track-files runner updates)
          (let [listing @(.listing runner)
                files-to-reload (into (dependents-of listing updates) updates)
                files-to-reload (sort files-to-reload)]
            (report-message reporter (str "took " (str-time-since start-time) " to determine which files to reload."))
            (report-message reporter "reloading files:")
            (doseq [file files-to-reload] (report-message reporter (str "  " (.getCanonicalPath file))))
            (apply reload-files runner files-to-reload))
          (run-and-report runner reporter)
          (catch Exception e (.printStackTrace e))))
      (swap! (.results runner) (fn [_] [])))))

(deftype VigilantRunner [listing results]
  Runner
  (run-directories [this directories reporter]
    (let [scheduler (ScheduledThreadPoolExecutor. 1)
          configuration (config-bindings)
          runnable (fn [] (try (tick configuration) (catch Exception e (print-stack-trace e))))]
      (.scheduleWithFixedDelay scheduler runnable 0 500 TimeUnit/MILLISECONDS)
      (.awaitTermination scheduler Long/MAX_VALUE TimeUnit/SECONDS)
      0))

  (submit-description [this description]
    (run-description this description (active-reporter)))

  (run-description [this description reporter]
    (let [run-results (do-description description reporter)]
      (swap! results into run-results)))

  (run-and-report [this reporter]
    (report-runs reporter @results))

  Object
  (toString [this] (str "listing: " (apply str (interleave (repeat endl) @listing)))))

(defn new-vigilant-runner []
  (VigilantRunner. (atom {}) (atom [])))


(ns speclj.runner.vigilant-runner
  (:use
    [speclj.running :only ()]
    [speclj.util]
    [clojure.set :only (difference union)])
  (:import
    [speclj.running Runner]
    [java.io PushbackReader FileReader File]))

; Data types -----------------------------------------------------------------------------------------------------------

(deftype VigilantRunner [listing dir]
  Runner
  (run [this description reporter])
  (report [this reporter])
  Object
  (toString [this] (str "on " dir endl "listing: " (apply str (interleave (repeat endl) @listing)))))

(defn new-vigilant-runner [dir]
  (VigilantRunner. (atom {}) dir))

(deftype FileTracker [mod-time dependencies]
  Object
  (toString [this] (str "mod-time: " mod-time " dependencies: " dependencies)))

(defn new-file-tracker
  [mod-time dependencies]
  (FileTracker. mod-time dependencies))

; Resolving ns names ---------------------------------------------------------------------------------------------------

(defn ns-to-filename [ns]
  (str (apply str (replace {\. \/ \- \_} (name ns))) ".clj"))

(defn ns-to-file [ns]
  (let [relative-filename (ns-to-filename ns)
        url (.getResource (ClassLoader/getSystemClassLoader) relative-filename)]
    (if (= "file" (.getProtocol url))
      (File. (.getFile url))
      nil)))

(defn- ns-form? [form]
  (and (list? form) (= 'ns (first form))))

(defn read-ns-form [file]
  (let [reader (PushbackReader. (FileReader. file))]
    (try
      (loop [form (read reader)]
        (if (ns-form? form)
          form
          (recur (read reader))))
      (catch Exception e nil)
      (finally (.close reader)))))

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

(defn- dependencies-of [file]
  (if-let [ns-form (read-ns-form file)]
    (let [dependency-names (dependencies-in-ns ns-form)]
      (vec (filter (complement nil?) (map #(ns-to-file %) dependency-names))))
    []))

(declare update-tracking-for-files)
(defn- update-tracking-for-file [listing file]
  (let [tracker (get listing key)]
    (if (not (or (nil? tracker) (modified? file tracker)))
      listing
      (let [dependencies (dependencies-of file)
            listing (apply update-tracking-for-files listing dependencies)]
        (assoc listing file (new-file-tracker (.lastModified file) dependencies))))))

(defn- update-tracking-for-files [listing & files]
  (loop [listing listing files files]
    (if (not (seq files))
      listing
      (recur (update-tracking-for-file listing (first files)) (rest files)))))

(defn track-file [runner & files]
  (swap! (.listing runner) #(apply update-tracking-for-files % files)))

(def clj-file-regex #".*\.clj")
(defn- clj-files [runner]
  (let [all-files (file-seq (.dir runner))]
    (filter #(re-matches clj-file-regex (.getName %)) all-files)))

(defn- dependency? [listing file]
  (some
    (fn [tracker] (some (partial = file) (.dependencies tracker)))
    (vals listing)))  

(defn- clean-deleted-files
  ([listing] (clean-deleted-files listing (filter #(not (.exists %)) (keys listing))))
  ([listing files-to-delete]
    (if (not (seq files-to-delete))
      listing
      (let [dependencies (reduce #(into %1 (.dependencies (get listing %2))) [] files-to-delete)
            listing (apply dissoc listing files-to-delete)
            unused-dependencies (filter #(not (dependency? listing %)) dependencies)]
        (clean-deleted-files listing unused-dependencies)))))

(defn updated-files [runner]
  (swap! (.listing runner) clean-deleted-files)
  (let [observed-files (set (clj-files runner))
        listing @(.listing runner)
        tracked-files (set (keys listing))
        new-files (difference observed-files tracked-files)
        modified-files (filter #(modified? % (get listing %)) tracked-files)]
    (concat new-files modified-files)))
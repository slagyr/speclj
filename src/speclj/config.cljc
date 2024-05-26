(ns speclj.config
  (:require [clojure.string :as str]
            [speclj.platform :as platform]))

(declare ^:dynamic *parent-description*)

(declare #^{:dynamic true} *reporters*)
(def default-reporters (atom nil))

(defn active-reporters []
  (if #?(:clj (bound? #'*reporters*) :cljs *reporters*)
    *reporters*
    (if-let [reporters @default-reporters]
      reporters
      (throw (new #?(:clj java.lang.Exception :cljs js/Error) "*reporters* is unbound and no default value has been provided")))))

(declare #^{:dynamic true} *runner*)
(def default-runner (atom nil))
(def default-runner-fn (atom nil))

(defn ^:export active-runner []
  (if #?(:clj (bound? #'*runner*) :cljs *runner*)
    *runner*
    (if-let [runner @default-runner]
      runner
      (throw (new #?(:clj java.lang.Exception :cljs js/Error)
                  "*runner* is unbound and no default value has been provided")))))

(declare #^{:dynamic true} *specs*)

(def #^{:dynamic true} *omit-pending?* false)

(def #^{:dynamic true} *color?* false)

(def #^{:dynamic true} *full-stack-trace?* false)

(def #^{:dynamic true} *tag-filter* {:include #{} :exclude #{}})

(def default-config
  {:specs        ["spec"]
   :runner       "standard"
   :reporters    ["progress"]
   :tags         []
   :omit-pending false})

#?(:clj
   (defn config-bindings
     "Returns a map of vars to values for all the ear-muffed vars in the speclj.config namespace.
     Can be used in (with-bindings ...) call to load a configuration state"
     []
     (let [ns              (the-ns 'speclj.config)
           all-vars        (dissoc (ns-interns ns) '*parent-description*)
           non-config-keys (remove #(str/starts-with? (name %) "*") (keys all-vars))
           config-vars     (apply dissoc all-vars non-config-keys)]
       (reduce #(assoc %1 %2 (deref %2)) {} (vals config-vars))))

   :cljs
   (defn config-bindings [] (throw "Not Supported in ClojureScript")))

(defn load-runner [name]
  (try
    (platform/dynamically-invoke (str "speclj.run." name) (str "new-" name "-runner"))
    (catch #?(:clj java.lang.Exception :cljs :default) e
      (throw (new #?(:clj java.lang.Exception :cljs js/Error) (str "Failed to load runner: " name) e)))))

(defn- load-reporter-by-name [name]
  (try
    (platform/dynamically-invoke (str "speclj.report." name) (str "new-" name "-reporter"))
    (catch #?(:clj java.lang.Exception :cljs :default) e
      (throw (new #?(:clj java.lang.Exception :cljs js/Error) (str "Failed to load reporter: " name) e)))))

(defn- load-reporter-by-name? [name-or-object]
  #?(:clj  (->> name-or-object
                (instance? (Class/forName "speclj.reporting.Reporter"))
                not)
     :cljs (string? name-or-object)))

(defn load-reporter [name-or-object]
  (cond-> name-or-object
          (load-reporter-by-name? name-or-object)
          load-reporter-by-name))

(defn- parse-tag [tag]
  (let [tag (name tag)]
    (if (str/starts-with? tag "~")
      [:excludes (str/replace tag #"^~" "")]
      [:includes tag])))

(defn- with-tag [tag-filter tag]
  (let [[flag value] (parse-tag tag)]
    (update tag-filter flag conj (keyword value))))

(defn parse-tags [tags]
  (reduce with-tag {:includes #{} :excludes #{}} tags))

#?(:clj
   (defn config-mappings [config]
     {#'*runner*            (if (:runner config) (load-runner (:runner config)) (active-runner))
      #'*reporters*         (if (:reporters config) (map load-reporter (:reporters config)) (active-reporters))
      #'*specs*             (:specs config)
      #'*color?*            (:color config)
      #'*omit-pending?*     (:omit-pending config)
      #'*full-stack-trace?* (some? (:stacktrace config))
      #'*tag-filter*        (parse-tags (:tags config))})

   :cljs
   (defn config-mappings [_] (throw "Not Supported in ClojureScript")))

(defn with-config
  "Runs the given function with all the configurations set.  Useful in cljs because config-mappings can't be used."
  [config action]
  (binding [*runner*            (if (:runner config) (do (println "loading runner in config") (load-runner (:runner config))) (active-runner))
            *reporters*         (if (:reporters config) (mapv load-reporter (:reporters config)) (active-reporters))
            *specs*             (:specs config)
            *color?*            (:color config)
            *omit-pending?*     (:omit-pending config)
            *full-stack-trace?* (some? (:stacktrace config))
            *tag-filter*        (parse-tags (:tags config))]
    (action)))

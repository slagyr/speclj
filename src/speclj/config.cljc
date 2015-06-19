(ns speclj.config
  (:require [speclj.platform :refer [dynamically-invoke print-stack-trace]]))

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

(defn active-runner []
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
  {:specs     ["spec"]
   :runner    "standard"
   :reporters ["progress"]
   :tags      []
   :omit-pending false})

#?(:clj
   (defn config-bindings
     "Retuns a map of vars to values for all the ear-muffed vars in the speclj.config namespace.
     Can be used in (with-bindings ...) call to load a configuration state"
     []
     (let [ns (the-ns 'speclj.config)
           all-vars (dissoc (ns-interns ns) '*parent-description*)
           non-config-keys (filter #(not (.startsWith (name %) "*")) (keys all-vars))
           config-vars (apply dissoc all-vars non-config-keys)]
       (reduce #(assoc %1 %2 (deref %2)) {} (vals config-vars))))

   :cljs
   (defn config-bindings [] (throw "Not Supported in ClojureScript")))

(defn load-runner [name]
  (try
    (dynamically-invoke (str "speclj.run." name) (str "new-" name "-runner"))
    (catch #?(:clj java.lang.Exception :cljs :default) e
      (throw (new #?(:clj java.lang.Exception :cljs js/Error) (str "Failed to load runner: " name) e)))))

(defn- load-reporter-by-name [name]
  (try
    (dynamically-invoke (str "speclj.report." name) (str "new-" name "-reporter"))
    (catch #?(:clj java.lang.Exception :cljs :default) e
      (throw (new #?(:clj java.lang.Exception :cljs js/Error) (str "Failed to load reporter: " name) e)))))

#?(:clj
   (defn load-reporter [name-or-object]
     (if (instance? (Class/forName "speclj.reporting.Reporter") name-or-object)
       name-or-object
       (load-reporter-by-name name-or-object)))

   :cljs
   (defn load-reporter [name-or-object]
     (if (string? name-or-object)
       (load-reporter-by-name name-or-object)
       name-or-object)))

(defn parse-tags [values]
  (loop [result {:includes #{} :excludes #{}} values values]
    (if (seq values)
      (let [value (name (first values))]
        (if (= \~ (first value))
          (recur (update-in result [:excludes] conj (keyword (apply str (rest value)))) (rest values))
          (recur (update-in result [:includes] conj (keyword value)) (rest values))))
      result)))

#?(:clj
   (defn config-mappings [config]
     {#'*runner*            (if (:runner config) (load-runner (:runner config)) (active-runner))
      #'*reporters*         (if (:reporters config) (map load-reporter (:reporters config)) (active-reporters))
      #'*specs*             (:specs config)
      #'*color?*            (:color config)
      #'*omit-pending?*     (:omit-pending config)
      #'*full-stack-trace?* (not (nil? (:stacktrace config)))
      #'*tag-filter*        (parse-tags (:tags config))})


   :cljs
   (defn config-mappings [_] (throw "Not Supportedin ClojureScript")))

(defn with-config
  "Runs the given function with all the cofigurations set.  Useful in cljs because config-mappings can't be used."
  [config action]
  (binding [*runner* (if (:runner config) (do (println "loading runner in config") (load-runner (:runner config))) (active-runner))
            *reporters* (if (:reporters config) (mapv load-reporter (:reporters config)) (active-reporters))
            *specs* (:specs config)
            *color?* (:color config)
            *omit-pending?* (:omit-pending config)
            *full-stack-trace?* (not (nil? (:stacktrace config)))
            *tag-filter* (parse-tags (:tags config))]
    (action)))

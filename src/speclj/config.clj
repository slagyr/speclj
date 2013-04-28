(ns speclj.config
  (:require [speclj.platform :refer [dynamically-invoke print-stack-trace]]))

(declare ^:dynamic *parent-description*)

(declare #^{:dynamic true} *reporters*)
(def default-reporters (atom nil))
(defn active-reporters []
  ;cljs-ignore->
  (if (bound? #'*reporters*)
    ;<-cljs-ignore
    ;cljs-include (if *reporters*
    *reporters*
    (if-let [reporters @default-reporters]
      reporters
      (throw (java.lang.Exception. "*reporters* is unbound and no default value has been provided")))))

(declare #^{:dynamic true} *runner*)
(def default-runner (atom nil))
(def default-runner-fn (atom nil))
(defn active-runner []
  ;cljs-ignore->
  (if (bound? #'*runner*)
    ;<-cljs-ignore
    ;cljs-include (if *runner*
    *runner*
    (if-let [runner @default-runner]
      runner
      (throw (java.lang.Exception. "*runner* is unbound and no default value has been provided")))))

(declare #^{:dynamic true} *specs*)

(def #^{:dynamic true} *color?* false)

(def #^{:dynamic true} *full-stack-trace?* false)

(def #^{:dynamic true} *tag-filter* {:include #{} :exclude #{}})

(def default-config {
                      :specs ["spec"]
                      :runner "standard"
                      :reporters ["progress"]
                      :tags []
                      })

;cljs-ignore->
(defn config-bindings
  "Retuns a map of vars to values for all the ear-muffed vars in the speclj.config namespace.
  Can be used in (with-bindings ...) call to load a configuration state"
  []
  (let [ns (the-ns 'speclj.config)
        all-vars (ns-interns ns)
        non-config-keys (filter #(not (.startsWith (name %) "*")) (keys all-vars))
        config-vars (apply dissoc all-vars non-config-keys)]
    (reduce #(assoc %1 %2 (deref %2)) {} (vals config-vars))))
;<-cljs-ignore

(defn load-runner [name]
  (try
    (dynamically-invoke (str "speclj.run." name) (str "new-" name "-runner"))
    (catch java.lang.Exception e (throw (java.lang.Exception. (str "Failed to load runner: " name) e)))))

(defn load-reporter [name]
  (try
    (dynamically-invoke (str "speclj.report." name) (str "new-" name "-reporter"))
    (catch java.lang.Exception e (throw (java.lang.Exception. (str "Failed to load reporter: " name) e)))))

(defn parse-tags [values]
  (loop [result {:includes #{} :excludes #{}} values values]
    (if (seq values)
      (let [value (name (first values))]
        (if (= \~ (first value))
          (recur (update-in result [:excludes] conj (keyword (apply str (rest value)))) (rest values))
          (recur (update-in result [:includes] conj (keyword value)) (rest values))))
      result)))

;cljs-ignore->
(defn config-mappings [config]
  {#'*runner* (if (:runner config) (load-runner (:runner config)) (active-runner))
   #'*reporters* (if (:reporters config) (map load-reporter (:reporters config)) (active-reporters))
   #'*specs* (:specs config)
   #'*color?* (:color config)
   #'*full-stack-trace?* (not (nil? (:stacktrace config)))
   #'*tag-filter* (parse-tags (:tags config))})
;<-cljs-ignore

(defn with-config
  "Runs the given function with all the cofigurations set.  Useful in cljs because config-mappings can't be used."
  [config action]
  (binding [*runner* (if (:runner config) (do (println "loading runner in config") (load-runner (:runner config))) (active-runner))
            *reporters* (if (:reporters config) (mapv load-reporter (:reporters config)) (active-reporters))
            *specs* (:specs config)
            *color?* (:color config)
            *full-stack-trace?* (not (nil? (:stacktrace config)))
            *tag-filter* (parse-tags (:tags config))]
    (action)))

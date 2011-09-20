(ns speclj.config)

(declare *reporters*)
(def default-reporters (atom nil))
(defn active-reporters []
  (if (bound? #'*reporters*)
    *reporters*
    (if-let [reporters @default-reporters]
      reporters
      (throw (Exception. "*reporters* is unbound and no default value has been provided")))))

(declare *runner*)
(def default-runner (atom nil))
(defn active-runner []
  (if (bound? #'*runner*)
    *runner*
    (if-let [runner @default-runner]
      runner
      (throw (Exception. "*runner* is unbound and no default value has been provided")))))

(declare *specs*)

(def *color?* false)

(def *full-stack-trace?* false)

(def *tag-filter* {:include #{} :exclude #{}})

(def default-config {
  :specs ["spec"]
  :runner "standard"
  :reporters ["progress"]
  :tags []
  })

(defn config-bindings
  "Retuns a map of vars to values for all the ear-muffed vars in the speclj.config namespace.
  Can be used in (with-bindings ...) call to load a configuration state"
  []
  (let [ns (the-ns 'speclj.config)
        all-vars (ns-interns ns)
        non-config-keys (filter #(not (.startsWith (name %) "*")) (keys all-vars))
        config-vars (apply dissoc all-vars non-config-keys)]
    (reduce #(assoc %1 %2 (deref %2)) {} (vals config-vars))))

(defn load-runner [name]
  (let [ns-name (symbol (str "speclj.run." name))
        ctor-name (symbol (str ns-name "/new-" name "-runner"))
        expr `(do (require '~ns-name) (~ctor-name))]
    (try
      (eval expr)
      (catch Exception e (throw (Exception. (str "Failed to load runner: " name) e))))))

(defn load-reporter [name]
  (let [ns-name (symbol (str "speclj.report." name))
        ctor-name (symbol (str ns-name "/new-" name "-reporter"))
        expr `(do (require '~ns-name) (~ctor-name))]
    (try
      (eval expr)
      (catch Exception e (throw (Exception. (str "Failed to load reporter: " name) e))))))

(defn parse-tags [values]
  (loop [result {:includes #{} :excludes #{}} values values]
    (if (seq values)
      (let [value (name (first values))]
        (if (= \~ (first value))
          (recur (update-in result [:excludes] conj (keyword (apply str (rest value)))) (rest values))
          (recur (update-in result [:includes] conj (keyword value)) (rest values))))
      result)))

(defn config-mappings [config]
  {#'*runner* (if (:runner config) (load-runner (:runner config)) (active-runner))
   #'*reporters* (if (:reporters config) (map load-reporter (:reporters config)) (active-reporters))
   #'*specs* (:specs config)
   #'*color?* (:color config)
   #'*full-stack-trace?* (not (nil? (:stacktrace config)))
   #'*tag-filter* (parse-tags (:tags config))})

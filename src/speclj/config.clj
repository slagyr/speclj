(ns speclj.config)

(declare *reporter*)
(def default-reporter(atom nil))
(defn active-reporter []
  (if (bound? #'*reporter*)
    *reporter*
    (if-let [reporter @default-reporter]
      reporter
      (throw (Exception. "*reporter* is unbound and no default value has been provided")))))

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

(defn config-bindings
  "Retuns a map of vars to values for all the ear-muffed vars in the speclj.config namespace.
  Can be used in (with-bindings ...) call to load a configuration state"
  []
  (let [ns (the-ns 'speclj.config)
        all-vars (ns-interns ns)
        non-config-keys (filter #(not (.startsWith (name %) "*")) (keys all-vars))
        config-vars (apply dissoc all-vars non-config-keys)]
    (reduce #(assoc %1 %2 (deref %2)) {} (vals config-vars))))

(def *full-stack-trace?* false)
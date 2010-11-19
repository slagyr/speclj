(ns speclj.main)

(def default-config {
  :spec-dirs ["spec"]
  :runner "standard"
  :reporter "console"
  })

(defn- parse-arg [config arg]
  (cond
    (.startsWith arg "--runner=") (assoc config :runner (.substring arg (count "--runner=")))
    (.startsWith arg "--reporter=") (assoc config :reporter (.substring arg (count "--reporter=")))
    :else (assoc config :spec-dirs (conj (vec (:spec-dirs config)) arg))))

(defn parse-args [& args]
  (loop [config {} args args]
    (if (not (seq args))
      (merge default-config config)
      (recur (parse-arg config (first args)) (rest args)))))

(defn load-runner [name]
  (let [ns-name (symbol (str "speclj.run." name))
        ctor-name (symbol (str ns-name "/new-" name "-runner"))
        expr `(do (require '~ns-name)(~ctor-name))]
    (try
      (eval expr)
      (catch Exception e (throw (Exception. (str "Failed to load runner: " name) e))))))

(defn load-reporter [name]
  (let [ns-name (symbol (str "speclj.report." name))
        ctor-name (symbol (str ns-name "/new-" name "-reporter"))
        expr `(do (require '~ns-name)(~ctor-name))]
    (try
      (eval expr)
      (catch Exception e (throw (Exception. (str "Failed to load reporter: " name) e))))))

(defn run [& args]
  (println "run args: " args))

(if *command-line-args*
  (run *command-line-args*))

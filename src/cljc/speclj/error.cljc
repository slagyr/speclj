(ns speclj.error)

(def pending :speclj.core/spec-pending)
(def failure :speclj.core/spec-failure)

(defn- ex-data-type? [e type] (= type (:type (ex-data e))))
(defn pending? [e] (ex-data-type? e pending))
(defn failure? [e] (ex-data-type? e failure))

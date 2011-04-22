(ns speclj.exec)

(deftype PassResult [characteristic seconds])
(deftype FailResult [characteristic seconds failure])
(deftype PendingResult [characteristic seconds])

(defn pass-result [characteristic seconds]
  (PassResult. characteristic seconds))

(defn fail-result [characteristic seconds failure]
  (FailResult. characteristic seconds failure))

(defn pending-result [characteristic seconds]
  (PendingResult. characteristic seconds))

(defmulti pass? type)
(defmethod pass? PassResult [result] true)
(defmethod pass? :default [result] false)

(defmulti fail? type)
(defmethod fail? FailResult [result] true)
(defmethod fail? :default [result] false)

(defmulti pending? type)
(defmethod pending? PendingResult [result] true)
(defmethod pending? :default [result] false)

(defn fail-count [results]
  (reduce #(if (fail? %2) (inc %) %) 0 results))
(ns speclj.results)

(deftype PassResult [characteristic seconds assertions])
(deftype FailResult [characteristic seconds failure assertions])
(deftype PendingResult [characteristic seconds exception])
(deftype ErrorResult [characteristic seconds exception])

(defn pass-result [characteristic seconds assertions]
  (PassResult. characteristic seconds assertions))

(defn fail-result [characteristic seconds failure assertions]
  (FailResult. characteristic seconds failure assertions))

(defn pending-result [characteristic seconds exception]
  (PendingResult. characteristic seconds exception))

(defn error-result [exception]
  (ErrorResult. nil 0 exception))

(defn pass? [result] (= (type result) PassResult))
(defn fail? [result] (= (type result) FailResult))
(defn pending? [result] (= (type result) PendingResult))
(defn error? [result] (= (type result) ErrorResult))
(defn failure? [result] (or (fail? result) (error? result)))
(defn fail-count [results] (count (filter failure? results)))

(defn categorize [results]
  (reduce (fn [tally result]
            (cond
              (pending? result) (update tally :pending conj result)
              (error? result) (update tally :error conj result)
              (fail? result) (update tally :fail conj result)
              :else (update tally :pass conj result)))
          {:pending [] :fail [] :pass [] :error []}
          results))

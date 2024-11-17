(ns speclj.results)

(deftype PassResult [characteristic seconds])
(deftype FailResult [characteristic seconds failure])
(deftype PendingResult [characteristic seconds exception])
(deftype ErrorResult [characteristic seconds exception])

(defn pass-result [characteristic seconds]
  (PassResult. characteristic seconds))

(defn fail-result [characteristic seconds failure]
  (FailResult. characteristic seconds failure))

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
              (pending? result) (update-in tally [:pending] conj result)
              (error? result) (update-in tally [:error] conj result)
              (fail? result) (update-in tally [:fail] conj result)
              :else (update-in tally [:pass] conj result)))
          {:pending [] :fail [] :pass [] :error []}
          results))
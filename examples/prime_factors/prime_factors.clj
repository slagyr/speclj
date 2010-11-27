(ns prime-factors
  (:use
    [speclj.core]))

(defn factors-of [n]
  (loop [factors [] divisor 2 n n]
    (if (<= n 1)
      factors
      (if (= 0 (mod n divisor))
        (recur (conj factors divisor) divisor (/ n divisor))
        (recur factors (inc divisor) n)))))

(describe "prime factors"
  (it "factors 1"
    (should= [] (factors-of 1)))

  (it "factors 2"
    (should= [2] (factors-of 2)))

  (it "factors 3"
    (should= [3] (factors-of 3)))

  (it "factors 4"
    (should= [2 2] (factors-of 4)))

  (it "factors 5"
    (should= [5] (factors-of 5)))

  (it "factors 6"
    (should= [2 3] (factors-of 6)))

  (it "factors 7"
    (should= [7] (factors-of 7)))

  (it "factors 8"
    (should= [2 2 2] (factors-of 8)))

  (it "factors 9"
    (should= [3 3] (factors-of 9)))

  (it "factors 2^100"
    (should= (repeat 100 2) (factors-of (Math/pow 2 100))))

; MDM - This one takes a bit too long to participate in the spec suite
;  (it "factors 2^19-1"
;    (let [mercene (int (- (Math/pow 2 19) 1))]
;      (should= [mercene] (factors-of mercene))))
  )


(run-specs)
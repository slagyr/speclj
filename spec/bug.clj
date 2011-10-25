(ns bug)

;(defmacro with []
;  (let [var-name (with-meta 'something {:dynamic true})]
;    `(do
;       (let [with-component# (atom 42)]
;         (declare ~var-name)
;         with-component#))))

;(defmacro with []
;  (let [var-name (with-meta 'something {:dynamic true})]
;    `(do
;       (declare something)
;       (reset-meta! (resolve something) (assoc (meta (resolve something)) :dynamic true))
;       (let [with-component# (atom 42)]
;         with-component#))))
;
;(list
;  (def _with_ (with))
;  (def _example_ (fn [] @something))
;  )
;
;(with-bindings {#'something _with_}
;;  (prn (ns-publics *ns*))              ; {... something #'bug/something ...}
;;  (prn (ns-resolve *ns* 'something))   ; #'bug/something
;;  (prn @(ns-resolve *ns* 'something))  ; #<Atom@508aeb74: 42>
;;  (prn @@(ns-resolve *ns* 'something)) ; 42
;;  (prn @something)                     ; 42
;  (_example_)
;  )

(list

   (declare ^:dynamic p)
;   (defn q [] @p)
   )

;(describe "working"
;  (with foo "bar")
;  (it "works" (= "bar" @foo)))

(binding [p (atom 10)]
  @p)
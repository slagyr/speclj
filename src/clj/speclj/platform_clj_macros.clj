(ns speclj.platform-clj-macros)

(defmacro when-not-bound [name & body]
    `(when-not (bound? (find-var '~name)) ~@body))

(defn -make-with [name body ctor bang?]
    (let [var-name (with-meta (symbol name) {:dynamic true})
                  unique-name (gensym "with")]
          `(do
                      (declare ~var-name)
                    (def ~unique-name (~ctor '~var-name '~unique-name (fn [] ~@body) ~bang?))
                    ~unique-name)))

(defmacro expected-larger-than-delta [expected actual delta]
    `(> (.abs (- (bigdec ~expected) (bigdec ~actual))) (.abs (bigdec ~delta))))

(def throwable 'Throwable)

;(defmacro new-throwable
;    ([] `(java.lang.Throwable.))
;    ([message] `(java.lang.Throwable. ~message)))

;(defmacro new-exception
;    ([] `(java.lang.Exception.))
;    ([message] `(java.lang.Exception. ~message))
;    ([message cause] `(java.lang.Exception. ~message ~cause)))

;(defmacro new-failure [message] `(speclj.SpecFailure. ~message))
;(defmacro new-pending [message] `(speclj.SpecPending. ~message))
;(defmacro throw-error [message] `(throw (Exception. ~message)))

(ns speclj.platform-macros)

(defmacro when-not-bound [name & body]
  `(when-not ~name ~@body))

(defn -make-with [name body ctor bang?]
  (let [var-name (with-meta (symbol name) {:dynamic true})
        munged-name (with-meta (symbol (cljs.compiler/munge (str name))) {:dynamic true})
        unique-name (gensym "with")]
    `(do
       (declare ~var-name)
       (def ~unique-name (~ctor '~munged-name '~unique-name (fn [] ~@body) ~bang?))
       ~unique-name)))

(def throwable 'js/Object)

(defmacro new-throwable
  ([] `(js/Error.))
  ([message] `(js/Error. ~message)))
(defmacro new-exception
  ([message] `(js/Error. ~message))
  ([message cause] `(js/Error. ~message)))
(defmacro new-failure [message] `(speclj.platform.SpecFailure. ~message))
(defmacro new-pending [message] `(speclj.platform.SpecPending. ~message))
(defmacro throw-error [message] `(throw (js/Error. ~message)))


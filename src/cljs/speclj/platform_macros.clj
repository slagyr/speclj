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

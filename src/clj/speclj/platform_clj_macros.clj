(ns speclj.platform-clj-macros)

;(defn -make-with [name body ctor bang?]
;    (let [var-name (with-meta (symbol name) {:dynamic true})
;          munged-name (if (cljs?)
;                        (with-meta (symbol (cljs.compiler/munge (str name))) {:dynamic true})
;                        var-name)
;          unique-name (gensym "with")]
;      `(do
;         (declare ~var-name)
;         (def ~unique-name (~ctor '~munged-name '~unique-name (fn [] ~@body) ~bang?))
;         ~unique-name)))

(defn -make-with [name body ctor bang?]
    (let [var-name (with-meta (symbol name) {:dynamic true})
          unique-name (gensym "with")]
      `(do
         (declare ~var-name)
         (def ~unique-name (~ctor '~var-name '~unique-name (fn [] ~@body) ~bang?))
         ~unique-name)))

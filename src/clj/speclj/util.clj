(ns speclj.util)

(defn compiling-cljs? []
  (boolean (find-ns 'cljs.analyzer)))

;(defn compiling-cljs? []
;  (boolean
;    (when-let [n (find-ns 'cljs.analyzer)]
;      (when-let [v (ns-resolve n '*cljs-file*)]
;        @v))))

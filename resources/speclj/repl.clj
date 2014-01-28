(ns speclj.repl
  (:require [cemerick.piggieback :as piggieback]))

;(defn browser-repl []
;  (println "Starting browser repl on port 9000")
;  (println "[don't forget to load the browser page!]")
;  (require 'cljs.repl.browser)
;  (piggieback/cljs-repl
;    :repl-env (doto ((ns-resolve 'cljs.repl.browser 'repl-env) :port 9000)
;                cljs.repl/-setup)))

(defn rhino-repl []
  (piggieback/cljs-repl))

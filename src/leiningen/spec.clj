(ns leiningen.spec
  (:use
    [leiningen.compile :only [eval-in-project]]))

(defn spec [project & args]
  (let [args (cons "-c" args)
        exec-form `(binding [~'invoke-method "lein spec"] (~'run ~@args))]
    (eval-in-project project exec-form nil nil `(use 'speclj.main))))

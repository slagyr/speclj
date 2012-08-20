(ns leiningen.new.speclj
  (:use [leiningen.new.templates :only [renderer name-to-path ->files]]))

(def render (renderer "speclj"))

(defn speclj
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)}]
    (->files data
             ["project.clj" (render "project.clj" data)]
             ["README.md" (render "README.md" data)]
             [".gitignore" (render ".gitignore" data)]
             ["src/{{sanitized}}/core.clj" (render "core.clj" data)]
             ["spec/{{sanitized}}/core_spec.clj" (render "core_spec.clj" data)])))

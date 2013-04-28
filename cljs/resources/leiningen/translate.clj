(ns leiningen.translate
  (:require [clojure.java.io :refer [copy file]]))

(defn remove-cljs-ignores [src]
  (.replaceAll
    (.matcher
      (java.util.regex.Pattern/compile ";cljs-ignore->.*?;<-cljs-ignore"
        (bit-or java.util.regex.Pattern/MULTILINE java.util.regex.Pattern/DOTALL))
      src)
    ""))

(defn translate-file [from to]
  (let [to-f (file to)
        src (slurp from)]
    (println (format "copying %s to %s" from to))
    (.mkdirs (.getParentFile to-f))
    (copy
      (-> src
        (.replace "speclj" "specljs")
        (.replace "(:require ;cljs-macros" "(:require-macros")
        (.replace "java.lang.Throwable" "js/Object")
        (.replace "java.lang.Exception" "js/Error")
        (.replace "java.lang.Object" "js/Object")
        (.replace ";cljs-include " "")
        remove-cljs-ignores)
      to-f)))

(def translations
  {"../src/speclj/components.clj" "src/translated/specljs/components.cljs"
   "../src/speclj/config.clj" "src/translated/specljs/config.cljs"
   "../src/speclj/core.clj" "src/translated/specljs/core.clj"
   "../src/speclj/report/documentation.clj" "src/translated/specljs/report/documentation.cljs"
   "../src/speclj/report/progress.clj" "src/translated/specljs/report/progress.cljs"
   "../src/speclj/report/silent.clj" "src/translated/specljs/report/silent.cljs"
   "../src/speclj/reporting.clj" "src/translated/specljs/reporting.cljs"
   "../src/speclj/results.clj" "src/translated/specljs/results.cljs"
   "../src/speclj/running.clj" "src/translated/specljs/running.cljs"
   "../src/speclj/tags.clj" "src/translated/specljs/tags.cljs"
   "../src/speclj/version.clj" "src/translated/specljs/version.cljs"
   ; Specs
   "../spec/speclj/config_spec.clj" "spec/translated/specljs/config_spec.cljs"
   "../spec/speclj/core_spec.clj" "spec/translated/specljs/core_spec.cljs"
   "../spec/speclj/report/documentation_spec.clj" "spec/translated/specljs/report/documentation_spec.cljs"
   "../spec/speclj/report/progress_spec.clj" "spec/translated/specljs/report/progress_spec.cljs"
   "../spec/speclj/reporting_spec.clj" "spec/translated/specljs/reporting_spec.cljs"
   "../spec/speclj/should_spec.clj" "spec/translated/specljs/should_spec.cljs"
   "../spec/speclj/spec_helper.clj" "spec/translated/specljs/spec_helper.clj"
   "../spec/speclj/tags_spec.clj" "spec/translated/specljs/tags_spec.cljs"
   })

(defn translate [project]
  (doseq [[to from] translations]
    (translate-file to from)))
(ns speclj.freshener-spec
  (:require
    [clojure.tools.namespace.dir :as dir]
    [clojure.tools.namespace.repl :as repl]
    [speclj.core :refer :all]
    [speclj.freshener :refer :all]
    [speclj.io :as io]))

(def sample-dir (io/canonical-file (io/as-file "examples/sample")))

(defn write-file [dir name content]
  (let [file (io/as-file dir name)]
    (io/make-parents file)
    (io/copy (io/make-input-stream (.getBytes content) {}) file)
    file))

(describe "Freshener"

  (it "finds specified files by default"
    (write-file sample-dir "portable.cljx" "I'm antiquated")
    (let [files (find-files-in #".*\.cljx" sample-dir)]
      (should-contain "portable.cljx" (set (map #(.getName %) files)))))

  (it "first freshening adds files to listing"
    (write-file sample-dir "a.clj" "I'm a clojure file")
    (write-file sample-dir "b.cljc" "I'm a clojure common file")
    (write-file sample-dir "c.cljx" "I'm neither")
    (let [files (clj-files-in sample-dir)]
      (should-contain "a.clj" (set (map #(.getName %) files)))
      (should-contain "b.cljc" (set (map #(.getName %) files)))
      (should-not-contain "c.cljx" (set (map #(.getName %) files)))))

  (context "freshen"
    (before
      (repl/set-refresh-dirs sample-dir))

    (it "new files are detected and added to tracker"
      (repl/clear)
      (freshen)
      (should= 2 (count (::dir/files repl/refresh-tracker)))
      (should-contain "a.clj" (set (map #(.getName %) (::dir/files repl/refresh-tracker))))
      (should-contain "b.cljc" (set (map #(.getName %) (::dir/files repl/refresh-tracker)))))

    (it "refresh dirs are updated to nil indicating the classpath"
      (freshen)
      (should= nil repl/refresh-dirs)))

  (repl/clear))

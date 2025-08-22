(ns speclj.freshener-spec
  (:require [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.track :as track]
            [speclj.core :refer :all]
            [speclj.freshener :as sut]
            [speclj.io :as io]
            [speclj.platform :as platform]))

(def sample-dir (io/canonical-file (io/as-file "examples/sample")))
(defonce test-tracker nil)
(def created-files (atom []))

(defn write-file [dir name content]
  (let [file (io/as-file dir name)]
    (io/make-parents file)
    (spit file content)
    (swap! created-files conj file)
    file))

(def clj-platform {:read-opts  {:read-cond :allow
                                :features  #{:clj}}
                   :extensions [".clj" ".cljc"]})

(def bb-platform {:read-opts  {:read-cond :allow
                               :features  #{:bb :clj}}
                  :extensions [".bb" ".clj" ".cljc"]})

(def cljs-platform {:read-opts  {:read-cond :allow
                                 :features  #{:cljs :clj}}
                    :extensions [".cljs" ".cljc"]})

(defn mock-scan []
  (alter-var-root #'test-tracker
                  #(dir/scan-dirs % [sample-dir] {:platform platform/find-platform})))

(describe "Freshener"
  (before (alter-var-root #'test-tracker (constantly (track/tracker))))
  (after (->> (reset-vals! created-files [])
              first
              (run! io/delete)))

  (it "finds specified files by default"
    (write-file sample-dir "portable.cljx" "I'm antiquated")
    (let [files (sut/find-files-in #".*\.cljx" sample-dir)]
      (should-contain "portable.cljx" (set (map io/file-name files)))))

  (it "first freshening adds files to listing"
    (write-file sample-dir "a.clj" "(ns sample.a)")
    (write-file sample-dir "b.cljc" "(ns sample.b)")
    (write-file sample-dir "c.cljx" "(ns sample.c)")
    (let [files (sut/clj-files-in [sample-dir] clj-platform)]
      (should-contain "a.clj" (set (map io/file-name files)))
      (should-contain "b.cljc" (set (map io/file-name files)))
      (should-not-contain "c.cljx" (set (map io/file-name files)))))

  (it "prefers extensions according to the supplied platform"
    (write-file sample-dir "a.clj" "(ns sample.a)")
    (write-file sample-dir "a.bb" "(ns sample.a)")
    (write-file sample-dir "a.cljc" "(ns sample.a)")
    (should= ["a.clj"] (map io/file-name (sut/clj-files-in [sample-dir] clj-platform)))
    (should= ["a.bb"] (map io/file-name (sut/clj-files-in [sample-dir] bb-platform)))
    (should= ["a.cljc"] (map io/file-name (sut/clj-files-in [sample-dir] cljs-platform))))

  (it "new files are detected and added to tracker"
    (write-file sample-dir "a.clj" "(ns sample.a)")
    (write-file sample-dir "b.cljc" "(ns sample.b)")
    (mock-scan)
    (sut/make-fresh! #'test-tracker)
    (should= 2 (count (::dir/files test-tracker)))
    (should-contain "a.clj" (set (map io/file-name (::dir/files test-tracker))))
    (should-contain "b.cljc" (set (map io/file-name (::dir/files test-tracker)))))
  )

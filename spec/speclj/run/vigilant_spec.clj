(ns speclj.run.vigilant-spec
  (:use
    [speclj.core]
    [speclj.run.vigilant]
    [clojure.java.io :only (file copy make-input-stream)]))

(def tmp-dir (file "_tmp"))
(def test-dir (file "_tmp/test"))
(def src-dir (file "_tmp/src"))
(def spec-dirs [(.getPath test-dir)])

(defn delete-tmp-dir []
  (doseq [file (reverse (file-seq tmp-dir))]
    (if (not (.delete file))
      (throw (str "Failed to delete file: " file)))))

(defn write-tmp-file [name content]
  (let [file (file tmp-dir name)]
    (copy (make-input-stream (.getBytes content) {}) file)
    file))

(defn tweak-mod-time [runner file tweak]
  (let [listing (.listing runner)
        tracker (get @listing file)]
    (if tracker
      (swap! listing assoc file (new-file-tracker (.ns tracker) (tweak (.mod-time tracker)) (.dependencies tracker))))))

(defn fake-ns-to-file [ns]
  (file src-dir (ns-to-filename ns)))

(describe "Vigilant Runner"
  (before (doseq [dir [tmp-dir test-dir src-dir]] (.mkdir dir)))
  (after (delete-tmp-dir))
  (with runner (new-vigilant-runner))
  (around [_] (binding [ns-to-file fake-ns-to-file] (_)))

  (it "detects no changes with empty directory"
    (should= 0 (count (updated-files @runner spec-dirs))))

  (it "detects changes on first check"
    (write-tmp-file "test/one.clj" "(ns one)")
    (let [updates (updated-files @runner spec-dirs)]
      (should= 1 (count updates))
      (should= "one.clj" (.getName (first updates)))))

  (it "detects changes new files"
    (updated-files @runner spec-dirs)
    (write-tmp-file "test/one.clj" "()")
    (let [updates (updated-files @runner spec-dirs)]
      (should= 1 (count updates))
      (should= "one.clj" (.getName (first updates)))))

  (it "detects changes on changed files"
    (let [tmp-file (write-tmp-file "test/one.clj" "(ns one)")]
      (track-files @runner tmp-file)
      (tweak-mod-time @runner tmp-file dec))
    (let [updates (updated-files @runner spec-dirs)]
      (should= 1 (count updates))
      (should= "one.clj" (.getName (first updates)))))

  (it "doesn't detect changes on unchanged files"
    (track-files @runner (write-tmp-file "test/one.clj" "(ns one)"))
    (should= 0 (count (updated-files @runner spec-dirs))))

  (it "detects file dependencies based on :use"
    (let [src-file (write-tmp-file "src/core.clj" "(ns core)")
          test-file (write-tmp-file "test/core-test.clj" "(ns core-test (:use [core]))")]
      (track-files @runner test-file)
      (tweak-mod-time @runner src-file dec)
      (tweak-mod-time @runner test-file dec))
    (let [updates (updated-files @runner spec-dirs)]
      (should= 2 (count updates))
      (should= "core-test.clj" (.getName (first (next updates))))
      (should= "core.clj" (.getName (first updates)))))

  (it "tracks empty files"
    (let [test-file (write-tmp-file "test/core-test.clj" "")]
      (track-files @runner test-file)
      (let [tracker (get @(.listing @runner) test-file)]
        (should-not= nil tracker)
        (should= nil (.ns tracker)))))

  (it "stops tracking files that have been deleted, along with their dependencies"
    (let [src-file1 (write-tmp-file "src/src1.clj" "(ns src1)")
          src-file2 (write-tmp-file "src/src2.clj" "(ns src2)")
          test-file1 (write-tmp-file "test/test1.clj" "(ns test1 (:use [src1][src2]))")
          test-file2 (write-tmp-file "test/test2.clj" "(ns test2 (:use [src2]))")]
      (track-files @runner test-file1 test-file2)
      (should-not= nil (get @(.listing @runner) test-file1))
      (should-not= nil (get @(.listing @runner) test-file2))
      (should-not= nil (get @(.listing @runner) src-file1))
      (should-not= nil (get @(.listing @runner) src-file2))
      (.delete test-file1)
      (clean-deleted-files @runner)
      (should= 0 (count (updated-files @runner spec-dirs)))
      (should= nil (get @(.listing @runner) test-file1))
      (should= nil (get @(.listing @runner) src-file1))
      (should-not= nil (get @(.listing @runner) test-file2))
      (should-not= nil (get @(.listing @runner) src-file2))))

  (it "stops tracking files that have been deleted, along with their NESTED dependencies"
    (let [src-file1 (write-tmp-file "src/src1.clj" "(ns src1 (:use [src2]))")
          src-file2 (write-tmp-file "src/src2.clj" "(ns src2)")
          test-file1 (write-tmp-file "test/test1.clj" "(ns test1 (:use [src1]))")]
      (track-files @runner test-file1)
      (should-not= nil (get @(.listing @runner) test-file1))
      (should-not= nil (get @(.listing @runner) src-file1))
      (should-not= nil (get @(.listing @runner) src-file2))
      (.delete test-file1)
      (clean-deleted-files @runner)
      (should= 0 (count (updated-files @runner spec-dirs)))
      (should= nil (get @(.listing @runner) test-file1))
      (should= nil (get @(.listing @runner) src-file1))
      (should= nil (get @(.listing @runner) src-file2))))

  (it "finds dependents of a given file"
    (let [src-file1 (write-tmp-file "src/src1.clj" "(ns src1)")
          src-file2 (write-tmp-file "src/src2.clj" "(ns src2)")
          test-file1 (write-tmp-file "test/test1.clj" "(ns test1 (:use [src1][src2]))")
          test-file2 (write-tmp-file "test/test2.clj" "(ns test2 (:use [src2]))")]
      (track-files @runner test-file1 test-file2)
      (should= #{} (dependents-of @(.listing @runner) [test-file1]))
      (should= #{} (dependents-of @(.listing @runner) [test-file2]))
      (should= #{test-file1} (dependents-of @(.listing @runner) [src-file1]))
      (should= #{test-file1 test-file2} (dependents-of @(.listing @runner) [src-file2]))))
  
  (it "finds transitive depedants/dependencies of a file"
    (let [src-file1 (write-tmp-file "src/src1.clj" "(ns src1)")
          src-file2 (write-tmp-file "src/src2.clj" "(ns src2 (:use [src1]))")
          test-file1 (write-tmp-file "test/test1.clj" "(ns test1 (:use [src2]))")]
      (track-files @runner test-file1 test-file1)
      (should= 3 (count @(.listing @runner)))
      (should= #{} (dependents-of @(.listing @runner) [test-file1]))
      (should= #{test-file1} (dependents-of @(.listing @runner) [src-file2]))
      (should= #{test-file1 src-file2} (dependents-of @(.listing @runner) [src-file1]))))

  (it "pulls no ns form a file that doens't contain one"
    (should= nil (read-ns-form (write-tmp-file "test/one.clj" "()")))
    (should= nil (read-ns-form (write-tmp-file "test/one.clj" "; hello")))
    (should= nil (read-ns-form (write-tmp-file "test/one.clj" "; (ns blah)"))))

  (it "pulls read ns form from files"
    (should= '(ns blah) (read-ns-form (write-tmp-file "test/one.clj" "(ns blah)")))
    (should= '(ns foo) (read-ns-form (write-tmp-file "test/one.clj" "; blah\n(ns foo)")))
    (should= '(ns blah (:use [foo]) (:require [bar])) (read-ns-form (write-tmp-file "test/one.clj" "(ns blah (:use [foo])(:require [bar]))"))))

  (it "pulls dependencies out of ns form"
    (should= '#{blah} (depending-names-of '(ns foo (:use [blah]))))
    (should= '#{bar} (depending-names-of '(ns foo (:use [bar]))))
    (should= '#{fizz} (depending-names-of '(ns foo (:use fizz))))
    (should= '#{fizz} (depending-names-of '(ns foo (:require fizz))))
    (should= '#{one two three} (depending-names-of '(ns foo (:use [one] [two] [three]))))
    (should= '#{one two three} (depending-names-of '(ns foo (:require [one] [two] [three]))))
    (should= '#{root.one root.two} (depending-names-of '(ns foo (:use [root [one] [two]]))))
    (should= '#{root.one root.two} (depending-names-of '(ns foo (:require [root [one] [two]]))))
    (should= '#{one two} (depending-names-of '(ns foo (:use [one :only (foo)] [two :exclude (bar)]))))
    (should= '#{one two} (depending-names-of '(ns foo (:require [one :as o] [two :as t]))))
    (should= '#{one.two one.three} (depending-names-of '(ns foo (:use [one [two :only (foo)] [three :exclude (bar)]]))))
    (should= '#{one.two one.three} (depending-names-of '(ns foo (:require [one [two :as t] [three :as tr]]))))
    (should= '#{root.one.child.grandchild root.two} (depending-names-of '(ns foo (:use [root [one [child [grandchild]]] [two]]))))
    (should= '#{fizz} (depending-names-of '(ns foo (:require [fizz] :reload))))
    (should= '#{fizz} (depending-names-of '(ns foo (:use [fizz] :verbose))))
    )

  (it "converts ns names into filenames"
    (should= "foo.clj" (ns-to-filename "foo"))
    (should= "bar.clj" (ns-to-filename "bar"))
    (should= "foo/bar.clj" (ns-to-filename "foo.bar"))
    (should= "foo_bar.clj" (ns-to-filename "foo-bar"))
    (should= "foo_bar/fizz_bang.clj" (ns-to-filename "foo-bar.fizz-bang")))

  )

(run-specs)
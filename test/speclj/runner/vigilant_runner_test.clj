(ns speclj.runner.vigilant-runner-test
  (:use
    speclj.core
    [speclj.runner.vigilant-runner]
    [clojure.java.io :only (file copy make-input-stream)])
  (:import
    [java.io File])
  )

(def tmp-dir (file "_tmp"))
(def test-dir (file "_tmp/test"))
(def src-dir (file "_tmp/src"))

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
      (swap! listing assoc file (new-file-tracker (tweak (.mod-time tracker)) (.dependencies tracker))))))

(defn fake-ns-to-file [ns]
  (file src-dir (ns-to-filename ns)))

(describe "Vigilant Runner"
  (before (doseq [dir [tmp-dir test-dir src-dir]] (.mkdir dir)))
  (after (delete-tmp-dir))
  (with runner (new-vigilant-runner test-dir))
  (around [_] (binding [ns-to-file fake-ns-to-file] (_)))

  (it "detects no changes with empty directory"
    (should= 0 (count (updated-files @runner))))

  (it "detects changes on first check"
    (write-tmp-file "test/one.clj" "()")
    (let [updates (updated-files @runner)]
      (should= 1 (count updates))
      (should= "one.clj" (.getName (first updates)))))

  (it "detects changes new files"
    (updated-files @runner)
    (write-tmp-file "test/one.clj" "()")
    (let [updates (updated-files @runner)]
      (should= 1 (count updates))
      (should= "one.clj" (.getName (first updates)))))

  (it "detects changes on changed files"
    (let [tmp-file (write-tmp-file "test/one.clj" "()")]
      (track-file @runner tmp-file)
      (tweak-mod-time @runner tmp-file dec))
    (let [updates (updated-files @runner)]
      (should= 1 (count updates))
      (should= "one.clj" (.getName (first updates)))))

  (it "doesn't detect changes on unchanged files"
    (track-file @runner (write-tmp-file "test/one.clj" "()"))
    (should= 0 (count (updated-files @runner))))

  (it "detects file dependencies based on :use"
    (let [src-file (write-tmp-file "src/core.clj" "")
          test-file (write-tmp-file "test/core-test.clj" "(ns (:use [core]))")]
      (track-file @runner test-file)
      (tweak-mod-time @runner src-file dec)
      (tweak-mod-time @runner test-file dec))
    (let [updates (updated-files @runner)]
      (should= 2 (count updates))
      (should= "core-test.clj" (.getName (first (next updates))))
      (should= "core.clj" (.getName (first updates)))))

  (it "stops tracking files that have been deleted, along with their dependencies"
    (let [src-file1 (write-tmp-file "src/src1.clj" "")
          src-file2 (write-tmp-file "src/src2.clj" "")
          test-file1 (write-tmp-file "test/test1.clj" "(ns (:use [src1][src2]))")
          test-file2 (write-tmp-file "test/test2.clj" "(ns (:use [src2]))")]
      (track-file @runner test-file1 test-file2)
      (should-not= nil (get @(.listing @runner) test-file1))
      (should-not= nil (get @(.listing @runner) test-file2))
      (should-not= nil (get @(.listing @runner) src-file1))
      (should-not= nil (get @(.listing @runner) src-file2))
      (.delete test-file1)
      (should= 0 (count (updated-files @runner)))
      (should= nil (get @(.listing @runner) test-file1))
      (should= nil (get @(.listing @runner) src-file1))
      (should-not= nil (get @(.listing @runner) test-file2))
      (should-not= nil (get @(.listing @runner) src-file2))))

  (it "stops tracking files that have been deleted, along with their NESTED dependencies"
    (let [src-file1 (write-tmp-file "src/src1.clj" "(ns (:use [src2]))")
          src-file2 (write-tmp-file "src/src2.clj" "")
          test-file1 (write-tmp-file "test/test1.clj" "(ns (:use [src1]))")]
      (track-file @runner test-file1)
      (should-not= nil (get @(.listing @runner) test-file1))
      (should-not= nil (get @(.listing @runner) src-file1))
      (should-not= nil (get @(.listing @runner) src-file2))
      (.delete test-file1)
      (should= 0 (count (updated-files @runner)))
      (should= nil (get @(.listing @runner) test-file1))
      (should= nil (get @(.listing @runner) src-file1))
      (should= nil (get @(.listing @runner) src-file2))))

  (it "pulls no ns form a file that doens't contain one"
    (should= nil (read-ns-form (write-tmp-file "test/one.clj" "()")))
    (should= nil (read-ns-form (write-tmp-file "test/one.clj" "; hello")))
    (should= nil (read-ns-form (write-tmp-file "test/one.clj" "; (ns blah)"))))

  (it "pulls read ns form from files"
    (should= '(ns blah) (read-ns-form (write-tmp-file "test/one.clj" "(ns blah)")))
    (should= '(ns foo) (read-ns-form (write-tmp-file "test/one.clj" "; blah\n(ns foo)")))
    (should= '(ns blah (:use [foo]) (:require [bar])) (read-ns-form (write-tmp-file "test/one.clj" "(ns blah (:use [foo])(:require [bar]))"))))

  (it "pulls dependencies out of ns form"
    (should= '#{blah} (dependencies-in-ns '(ns foo (:use [blah]))))
    (should= '#{bar} (dependencies-in-ns '(ns foo (:use [bar]))))
    (should= '#{fizz} (dependencies-in-ns '(ns foo (:use fizz))))
    (should= '#{fizz} (dependencies-in-ns '(ns foo (:require fizz))))
    (should= '#{one two three} (dependencies-in-ns '(ns foo (:use [one] [two] [three]))))
    (should= '#{one two three} (dependencies-in-ns '(ns foo (:require [one] [two] [three]))))
    (should= '#{root.one root.two} (dependencies-in-ns '(ns foo (:use [root [one] [two]]))))
    (should= '#{root.one root.two} (dependencies-in-ns '(ns foo (:require [root [one] [two]]))))
    (should= '#{one two} (dependencies-in-ns '(ns foo (:use [one :only (foo)] [two :except (bar)]))))
    (should= '#{one two} (dependencies-in-ns '(ns foo (:require [one :as o] [two :as t]))))
    (should= '#{one.two one.three} (dependencies-in-ns '(ns foo (:use [one [two :only (foo)] [three :except (bar)]]))))
    (should= '#{one.two one.three} (dependencies-in-ns '(ns foo (:require [one [two :as t] [three :as tr]]))))
    (should= '#{root.one.child.grandchild root.two} (dependencies-in-ns '(ns foo (:use [root [one [child [grandchild]]] [two]]))))
    )

  (it "converts ns names into filenames"
    (should= "foo.clj" (ns-to-filename "foo"))
    (should= "bar.clj" (ns-to-filename "bar"))
    (should= "foo/bar.clj" (ns-to-filename "foo.bar"))
    (should= "foo_bar.clj" (ns-to-filename "foo-bar"))
    (should= "foo_bar/fizz_bang.clj" (ns-to-filename "foo-bar.fizz-bang")))

  )

(conclude-single-file-run)
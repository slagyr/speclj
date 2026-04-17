(ns speclj.line-filter-spec
  (:require [speclj.components :as components]
            [speclj.core #?(:cljs :refer-macros :default :refer) [context describe it should= should-not]]
            [speclj.line-filter :as sut]))

(defn- mk-char [name file line]
  (components/new-characteristic name nil (fn [] :nop) false {:file file :line line}))

(defn- mk-desc [name file line]
  (components/new-description name false "test.ns" {:file file :line line}))

(defn- install-all [parent & kids]
  (doseq [k kids] (components/install k parent))
  parent)

(describe "line-filter"

  (context "split-line-spec"

    (it "splits path:line"
      (should= ["spec/foo.clj" 42] (sut/split-line-spec "spec/foo.clj:42")))

    (it "returns nil line for bare paths"
      (should= ["spec/foo.clj" nil] (sut/split-line-spec "spec/foo.clj"))
      (should= ["spec" nil] (sut/split-line-spec "spec"))
      (should= ["spec/" nil] (sut/split-line-spec "spec/")))

    (it "preserves Windows-style drive letters with line suffix"
      (should= ["C:\\spec\\foo.clj" 42] (sut/split-line-spec "C:\\spec\\foo.clj:42")))

    (it "rejects the '::N' double-colon form"
      (should= ["foo.clj::42" nil] (sut/split-line-spec "foo.clj::42")))

    (it "does not parse non-numeric :suffix as a line"
      (should= ["foo.clj:bar" nil] (sut/split-line-spec "foo.clj:bar"))))

  (context "resolve-targets"

    (it "returns empty chosen and no unmatched for empty targets"
      (should= {:chosen #{} :unmatched []} (sut/resolve-targets [] {})))

    (it "records unmatched when the file is not loaded"
      (let [result (sut/resolve-targets [] {"missing.clj" 10})]
        (should= #{} (:chosen result))
        (should= ["missing.clj:10"] (:unmatched result))))

    (it "selects the exact-line characteristic"
      (let [char-10 (mk-char "it-at-10" "foo.clj" 10)
            char-20 (mk-char "it-at-20" "foo.clj" 20)
            desc-3  (mk-desc "top" "foo.clj" 3)
            _       (install-all desc-3 char-10 char-20)
            {:keys [chosen unmatched]} (sut/resolve-targets [desc-3] {"foo.clj" 10})]
        (should= #{char-10} chosen)
        (should= [] unmatched)))

    (it "selects the nearest preceding characteristic for an interior line"
      (let [char-10 (mk-char "it-at-10" "foo.clj" 10)
            char-20 (mk-char "it-at-20" "foo.clj" 20)
            desc-3  (mk-desc "top" "foo.clj" 3)
            _       (install-all desc-3 char-10 char-20)
            {:keys [chosen]} (sut/resolve-targets [desc-3] {"foo.clj" 15})]
        (should= #{char-10} chosen)))

    (it "selects all descendants when the line hits the describe"
      (let [char-10 (mk-char "it-at-10" "foo.clj" 10)
            char-20 (mk-char "it-at-20" "foo.clj" 20)
            desc-3  (mk-desc "top" "foo.clj" 3)
            _       (install-all desc-3 char-10 char-20)
            {:keys [chosen]} (sut/resolve-targets [desc-3] {"foo.clj" 3})]
        (should= #{char-10 char-20} chosen)))

    (it "runs every characteristic when the line is above any construct"
      (let [char-10 (mk-char "it-at-10" "foo.clj" 10)
            char-20 (mk-char "it-at-20" "foo.clj" 20)
            desc-3  (mk-desc "top" "foo.clj" 3)
            _       (install-all desc-3 char-10 char-20)
            {:keys [chosen]} (sut/resolve-targets [desc-3] {"foo.clj" 1})]
        (should= #{char-10 char-20} chosen)))

    (it "matches by path suffix (user-supplied relative path)"
      (let [longer-file "/abs/path/to/foo.clj"
            char-x      (mk-char "x" longer-file 10)
            desc        (mk-desc "top" longer-file 3)
            _           (install-all desc char-x)
            {:keys [chosen]} (sut/resolve-targets [desc] {"foo.clj" 10})]
        (should= #{char-x} chosen)))

    (it "picks the innermost describe when the line hits it"
      (let [inner-char (mk-char "inner" "foo.clj" 30)
            inner-desc (mk-desc "inner-ctx" "foo.clj" 25)
            outer-char (mk-char "outer" "foo.clj" 10)
            outer-desc (mk-desc "outer" "foo.clj" 3)
            _          (install-all inner-desc inner-char)
            _          (install-all outer-desc outer-char inner-desc)
            {:keys [chosen]} (sut/resolve-targets [outer-desc] {"foo.clj" 25})]
        (should= #{inner-char} chosen)))

    (it "picks a specific it over its enclosing describe at the same line"
      (let [sib-char (mk-char "sib" "bar.clj" 5)
            sib-desc (mk-desc "sib-ctx" "bar.clj" 5)
            _        (install-all sib-desc sib-char)
            {:keys [chosen]} (sut/resolve-targets [sib-desc] {"bar.clj" 5})]
        (should= #{sib-char} chosen))))

  (context "pass-line-filter?"

    (it "passes everything when filter is inactive"
      (let [char-a (mk-char "a" "foo.clj" 10)
            desc   (mk-desc "top" "foo.clj" 3)
            _      (install-all desc char-a)]
        (binding [sut/*chosen-characteristics* nil]
          (should= true (sut/pass-line-filter? char-a))
          (should= true (sut/pass-line-filter? desc)))))

    (it "passes only chosen characteristics when filter is active"
      (let [char-a (mk-char "a" "foo.clj" 10)
            char-b (mk-char "b" "foo.clj" 20)
            desc   (mk-desc "top" "foo.clj" 3)
            _      (install-all desc char-a char-b)]
        (binding [sut/*chosen-characteristics* #{char-a}]
          (should= true (sut/pass-line-filter? char-a))
          (should-not (sut/pass-line-filter? char-b)))))

    (it "always passes descriptions when active"
      (let [char-a (mk-char "a" "foo.clj" 10)
            desc   (mk-desc "top" "foo.clj" 3)
            _      (install-all desc char-a)]
        (binding [sut/*chosen-characteristics* #{char-a}]
          (should= true (sut/pass-line-filter? desc))))))
  )

(ns speclj.platform-spec
  (:require #?@(:cljs [] :default [[clojure.tools.namespace.find :as find]])
            [speclj.core #?(:cljs :refer-macros :default :refer) [describe should-be context it should should-contain should-not should= should-throw]]
            [speclj.platform :as sut #?(:cljs :refer-macros :default :refer) [if-cljs try-catch-anything]]
            [speclj.run.standard :as standard]))

(defmacro which-env []
  (if-cljs :cljs :clj))

(defmacro test-enter-pressed [in result]
  `(it (pr-str ~in)
     (with-redefs [#?@(:cljr [sut/key-available? (constantly true)])]
       (with-in-str ~in (should= ~result (sut/enter-pressed?))))))

(defn ->stack-element [class-name]
  #?(:clj     (StackTraceElement. class-name "foo_method" (str class-name ".clj") 123)
     :default class-name))

(describe "platform-specific bits"
  #?(:cljs
     (it "javascript object stays pristine"
       (should= {} (js->clj (js-obj)))))

  (it "line-separator"
    (let [separator #?(:clj (System/getProperty "line.separator")
                       :cljs "\n"
                       :cljr Environment/NewLine)]
      (should-be string? sut/endl)
      (should= separator sut/endl)))

  (it "file-separator"
    (let [separator #?(:clj (System/getProperty "file.separator")
                       :cljs "/"
                       :cljr (str System.IO.Path/DirectorySeparatorChar))]
      (should-be string? sut/file-separator)
      (should= separator sut/file-separator)))

  (it "source-file-regex"
    (let [re #?(:clj ".*\\.clj(c)?"
                :cljs "/.*\\.clj(c|s)?/"
                :cljr ".*\\.clj(c|r)?")]
      (should-be sut/re? sut/source-file-regex)
      (should= re (str sut/source-file-regex))))

  (it "type-name"
    (should= #?(:clj  "java.lang.Exception"
                :cljr "System.Exception"
                :cljs "Error")
             (sut/type-name #?(:cljs js/Error :default Exception)))
    (should= #?(:clj  "java.lang.Object"
                :cljr "System.Object"
                :cljs "Object")
             (sut/type-name #?(:cljs js/Object :default Object))))

  (it "failure-source"
    (let [ex (ex-info "the failure" {})]
      #?(:clj  (should-contain #"^speclj/platform_spec.clj:\d+$" (sut/failure-source ex))
         :cljr (do
                 (try (throw ex) (catch Exception _))
                 (should= "speclj/platform_spec" (sut/failure-source ex))))))

  (it "elide-level?"
    (#?(:cljs should-not :default should) (sut/elide-level? (->stack-element "clojure.core.blah")))
    (#?(:cljs should-not :default should) (sut/elide-level? (->stack-element "speclj.core.blah")))
    (should-not (sut/elide-level? (->stack-element "specljs")))
    (should-not (sut/elide-level? (->stack-element "clojures"))))

  #?(:cljs (list)
     :default
     (context "non-cljs"

       (it "get-bytes"
         (should= [102 111 111] (sut/get-bytes "foo")))

       (context "enter-pressed?"
         (test-enter-pressed "\n" true)
         (test-enter-pressed "a" false)
         (test-enter-pressed "\t" false))
       ))

  #?(:clj
     (it "find-platform"
       (should= sut/find-platform find/clj))
     :cljr
     (it "find-platform"
       (should= sut/find-platform find/cljr)))

  (describe "if-cljs"
    (it "conditionally compiles a macro"
      (should= #?(:cljs :cljs :default :clj) (which-env))))

  (describe "try-catch-anything"
    (let [throwable #?(:clj (Throwable. "welp")
                       :cljs "welp"
                       :cljr (Exception. "welp"))]
      (it "catches anything"
        (try-catch-anything
          (throw throwable)
          (catch e
                 (should= e throwable))))

      (it "throws if the last form is not a catch"
        (should-throw
          (try-catch-anything
            :nope)))

      (it "throws if the binding is not a symbol"
        (should-throw
          (try-catch-anything
            :yep
            (catch :nope 'whatever))))))
  )

(standard/run-specs)

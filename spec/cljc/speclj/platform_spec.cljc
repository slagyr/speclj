(ns speclj.platform-spec
  (:require #?@(:cljs [] :default [[clojure.tools.namespace.find :as find]])
            [speclj.core #?(:cljs :refer-macros :default :refer) [describe should should-not should-be it should-not-be should-contain should= should-not= should-be-nil should-not-be-nil should-have-count should-throw should-fail xit]]
            [speclj.platform :as sut #?(:cljs :refer-macros :default :refer) [if-cljs try-catch-anything]]
            [speclj.run.standard :as standard]))

(defmacro which-env []
  (if-cljs :cljs :clj))

(defmacro result-or-ex [& body]
  `(try (do ~@body)
        (catch #?(:cljs :default
                  :cljr Exception
                  :default Throwable)
               e# e#)))

(defmacro current-line
  "Macroexpands to the line number of the call site under bb/JVM. Returns nil
   under cljs because same-file defmacros don't receive a useful &form there.
   Used together with assert-loc-at to pin the precise expected line on
   platforms where it works."
  [_marker]
  (-> &form meta :line))

(defn assert-loc-at
  "Coverage helper. Asserts the loc map `d` extracted from a failure's ex-data
   has the expected :file and :line. `expected-line` is either an integer (the
   precise expected line, used on bb/JVM) or :any to assert only that :line is
   a positive integer (used on cljs where current-line returns nil)."
  [d expected-line]
  (should-contain "speclj/platform_spec" (str (:file d)))
  (if (= :any expected-line)
    (should (and (number? (:line d)) (pos? (:line d))))
    (should= expected-line (:line d))))

(defn ->stack-element [class-name]
  #?(:clj     (StackTraceElement. class-name "foo_method" (str class-name ".clj") 123)
     :default class-name))

(defn caught
  "Evaluates `thunk` and returns the thrown exception, or nil if nothing is thrown."
  [thunk]
  (result-or-ex (thunk) nil))

(describe "platform-specific bits"
  #?(:cljs
     (it "javascript object stays pristine"
       (should= {} (js->clj (js-obj)))))

  (it "line-separator"
    (let [separator #?(:clj  (System/getProperty "line.separator")
                       :cljs "\n"
                       :cljr Environment/NewLine)]
      (should-be string? sut/endl)
      (should= separator sut/endl)))

  (it "file-separator"
    (let [separator #?(:clj  (System/getProperty "file.separator")
                       :cljs "/"
                       :cljr (str System.IO.Path/DirectorySeparatorChar))]
      (should-be string? sut/file-separator)
      (should= separator sut/file-separator)))

  (it "source-file-regex"
    (let [re #?(:bb   ".*\\.(cljc|clj|bb)"
                :clj  ".*\\.clj(c)?"
                :cljs "/.*\\.clj(c|s)/"
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
      #?(:cljr (try (throw ex) (catch Exception _)))
      ; Babashka doesn't know where runtime exceptions occur
      #?(:bb   (should-contain #"^sci/lang/Var.clj:\d+$" (sut/failure-source-str ex))
         :clj  (should-contain #"^speclj/platform_spec.clj:\d+$" (sut/failure-source-str ex))
         :cljr (should= "speclj/platform_spec" (sut/failure-source-str ex)))))

  (it "should-* failures embed file and precise line in ex-data"
    ;; speclj.core/-capture-loc embeds {:file :line :column} in the ex-info
    ;; thrown by every user-facing should-* macro. failure-source must prefer
    ;; that over the JVM/sci stack trace, otherwise babashka users see
    ;; sci/lang/Var.clj instead of their own source line.
    (let [base (current-line ::mark)
          ex   (result-or-ex (should= 1 2))   ; line = base + 1
          d    (ex-data ex)]
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))
      (should-contain "speclj/platform_spec" (sut/failure-source-str ex))))

  (it "pending embeds its own &form loc in ex-data"
    ;; The pending macro embeds its &form loc directly in the ex-info; without
    ;; this, babashka users see sci/lang/Var.clj instead of the pending line.
    (let [base (current-line ::mark)
          ex   (result-or-ex (speclj.core/pending "TODO"))   ; line = base + 1
          d    (ex-data ex)]
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))
      (should-contain "speclj/platform_spec" (sut/failure-source-str ex))))

  (it "every kind of should-* macro flows loc through help-should"
    ;; The data-driven sweep below exercises a representative from each
    ;; help-should-using arm of speclj.core. Each thunk's body is a one-line
    ;; failing form whose precise line we pin via (current-line ::mark) on
    ;; bb/JVM. (Under cljs current-line returns nil, so we only assert the
    ;; loc was embedded at all.)
    ;;
    ;; If any of these stops embedding :file/:line, exactly one row will fail
    ;; — making it cheap to add a new should-* macro and assert it's wired up.
    (let [base (current-line ::mark)
          rows [["should="            #(should= 1 2)                    1]
                ["should-not="        #(should-not= 1 1)                 2]
                ["should"             #(should false)                   3]
                ["should-not"         #(should-not true)                 4]
                ["should-be"          #(should-be neg? 1)                5]
                ["should-not-be"      #(should-not-be pos? 1)            6]
                ["should-contain"     #(should-contain :a {:b 1})        7]
                ["should-have-count"  #(should-have-count 9 [1 2 3])     8]]]
      (doseq [[_name thunk row-offset] rows]
        (let [ex (caught thunk)
              d  (ex-data ex)]
          (should-not-be-nil ex)
          (assert-loc-at d #?(:cljs :any :default (+ base row-offset)))))))

  (it "delegating should-be-nil reports its own call site, not the inner should="
    ;; should-be-nil expands to (with-meta `(should= nil ~form) (meta &form)).
    ;; The propagated meta is what makes should=' &form see the user's
    ;; should-be-nil call site instead of the syntax-generated form.
    (let [base (current-line ::mark)
          ex   (result-or-ex (should-be-nil 42))   ; line = base + 1
          d    (ex-data ex)]
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))))

  (it "delegating should-not-be-nil reports its own call site"
    (let [base (current-line ::mark)
          ex   (result-or-ex (should-not-be-nil nil))   ; line = base + 1
          d    (ex-data ex)]
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))))

  (it "0-arg should-fail propagates loc to the 1-arg arity"
    ;; (should-fail) is the (with-meta `(should-fail "Forced failure") ...) path.
    (let [base (current-line ::mark)
          ex   (result-or-ex (should-fail))   ; line = base + 1
          d    (ex-data ex)]
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))))

  (it "should-throw with no throw uses -new-failure and embeds *source-loc*"
    ;; When the body of (should-throw ...) completes without throwing,
    ;; -create-should-throw-failure builds an ex-info via -new-failure, which
    ;; merges *source-loc* (bound by the surrounding help-should) into ex-data.
    ;; This is the only path that exercises -new-failure rather than -fail.
    (let [base (current-line ::mark)
          ex   (result-or-ex (should-throw sut/throwable :nothing-thrown))   ; line = base + 1
          d    (ex-data ex)]
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))))

  (it "should-throw 3-arg uses with-source-loc and reports the should-throw line"
    ;; The 3-arg arity wraps its body with with-source-loc (no double-counting)
    ;; so the inner should=/should-not-be-nil delegations report the user's
    ;; should-throw call site rather than nothing at all.
    (let [base (current-line ::mark)
          ex   (result-or-ex (should-throw sut/throwable
                                           #"completely-different-message"
                                           (throw (ex-info "actual" {}))))   ; line = base + 3
          d    (ex-data ex)]
      ;; The wrapped body's location is the should-throw line itself (base+1),
      ;; because with-source-loc binds *source-loc* at expansion of should-throw.
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))))

  (it "standalone -fail outside any should-* captures its own &form loc"
    ;; speclj.core/-fail is documented as a public hook for custom assertions.
    ;; When called outside a help-should wrapper, *source-loc* is unbound, so
    ;; -fail must rely on its own (meta &form) to embed file/line.
    (let [base (current-line ::mark)
          ex   (result-or-ex (speclj.core/-fail "boom"))   ; line = base + 1
          d    (ex-data ex)]
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))))

  (it "body-less (it ...) auto-pending captures the it call site"
    ;; help-it body-less branch synthesizes (with-meta `(pending) (meta &form))
    ;; so the auto-pending characteristic reports the user's it line. The Char
    ;; is created but never installed; we invoke its body fn directly.
    (let [base    (current-line ::mark)
          chr     (it "auto-pending sentinel")    ; line = base + 1
          body-fn (.-body chr)
          ex      (result-or-ex (body-fn) nil)
          d       (ex-data ex)]
      (should-not-be-nil ex)
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))))

  (it "xit injects a (pending) carrying the xit call site"
    (let [base    (current-line ::mark)
          chr     (xit "skipped sentinel")        ; line = base + 1
          body-fn (.-body chr)
          ex      (result-or-ex (body-fn) nil)
          d       (ex-data ex)]
      (should-not-be-nil ex)
      (assert-loc-at d #?(:cljs :any :default (+ base 1)))))

  (it "elide-level?"
    (#?(:cljs should-not-be :default should-be) sut/elide-level? (->stack-element "clojure.core.blah"))
    (#?(:cljs should-not-be :default should-be) sut/elide-level? (->stack-element "speclj.core.blah"))
    (#?(:bb should-be :default should-not-be) sut/elide-level? (->stack-element "babashka.foo"))
    (#?(:bb should-be :default should-not-be) sut/elide-level? (->stack-element "sci.foo"))
    (#?(:bb should-be :default should-not-be) sut/elide-level? (->stack-element "edamame.foo"))
    (should-not-be sut/elide-level? (->stack-element "specljs"))
    (should-not-be sut/elide-level? (->stack-element "clojures")))

  #?(:cljs (list)
     :default
     (it "get-bytes"
       (should= [102 111 111] (sut/get-bytes "foo"))))

  #?(:bb
     (it "find-platform"
       (should= sut/find-platform {:read-opts {:read-cond :allow
                                               :features #{:bb :clj}}
                                   :extensions [".bb" ".clj" ".cljc"]}))
     :clj
     (it "find-platform"
       (should= sut/find-platform find/clj))
     :cljr
     (it "find-platform"
       (should= sut/find-platform find/cljr)))

  (it "if-cljs conditionally compiles a macro"
    (should= #?(:cljs :cljs :default :clj) (which-env)))

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

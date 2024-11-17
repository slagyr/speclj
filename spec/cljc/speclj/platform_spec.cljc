(ns speclj.platform-spec
  (:require [speclj.core #?(:cljs :refer-macros :default :refer) [describe it should= should-throw]]
            [speclj.platform #?(:cljs :refer-macros :default :refer) [if-cljs try-catch-anything]]
            [speclj.run.standard :as standard]))

(defmacro which-env []
  (if-cljs :cljs :clj))

(describe "platform-specific bits"
  #?(:cljs
     (it "javascript object stays pristine"
       (should= {} (js->clj (js-obj)))))

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
            (catch :nope 'whatever)))))))

(standard/run-specs)

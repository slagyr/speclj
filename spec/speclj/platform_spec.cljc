(ns speclj.platform-spec
  (:require [speclj.core #?(:clj :refer :cljs :refer-macros) [describe it should= should-throw]]
            [speclj.platform #?(:clj :refer :cljs :refer-macros) [if-cljs try-catch-anything]]
            [speclj.run.standard :as standard]))

(defmacro which-env []
  (if-cljs :cljs :clj))

(describe "platform-specific bits"
  #?(:cljs
     (it "javascript object stays pristine"
       (should= {} (js->clj (js-obj)))))

  (describe "if-cljs"
    (it "conditionally compiles a macro"
      (should= #?(:clj :clj :cljs :cljs) (which-env))))

  (describe "try-catch-anything"
    (let #?(:clj  [throwable (Throwable. "welp")]
            :cljs [throwable "welp"])
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

(ns speclj.stub
  (:require ;cljs-macros
            [speclj.platform :refer [throw-error]])
  (:require [speclj.platform :refer [endl]]))

(declare ^:dynamic *stubbed-invocations*)

(defn- check-recording []
    ;cljs-ignore->
    (when-not (bound? #'*stubbed-invocations*)
      ;<-cljs-ignore
      ;cljs-include (when-not *stubbed-invocations*
      (throw-error "Stub recoding not bound.  Please add (with-stubs) to the decribe/context.")))

  (defn -record-invocation [name args]
    (check-recording)
    (let [args (if (= nil args) [] (vec args))]
      (swap! *stubbed-invocations* conj [name args])))

  (defn- invoke-delegate [name delegate args]
    (try
      (apply delegate args)
    ;cljs-ignore->
      (catch clojure.lang.ArityException e
        (throw-error (str "Stub " name " was invoked with " (.-actual e) " arguments, but the :invoke fn has a different arity")))
      ;<-cljs-ignore
      ;cljs-include (catch speclj.platform/throwable e (throw-error (str "Error calling :invoke fn for stub " name ": " e)))
      ))

  (defn stub
    "Creates a stub function.  Each call to the stub will be recorded an can later be looked up using the specified name.

    Options:
      :invoke - a function that will be invoked when the stub is invoked.  All the arguments passed to the stub will
        be passed to the :invoke value and it's return value returned by the stub.
      :return - a value that will be returned by the stub.  This overides the result of the :invoke value, if specified.
      :throw - an exception that will be thrown by the stub."
    ([name] (stub name {}))
    ([name options]
      (let [delegate (:invoke options)]
        (when (and delegate (not (ifn? delegate)))
          (throw-error "stub's :invoke argument must be an ifn"))
        (fn [& args]
          (-record-invocation name args)
          (let [result (if delegate (invoke-delegate name delegate args) nil)]
            (when-let [throwable (:throw options)]
              (throw throwable))
            (if (contains? options :return)
              (:return options)
              result))))))

  (defn invocations-of
    "Returns a list of argument lists representing each invocation of the specified stub."
    [name]
    (map second
      (filter #(= name (first %))
        @*stubbed-invocations*)))

  (defn first-invocation-of
    "Returns the list of arguments passed into the first invocation of the specified stub, nil if it was never invoked."
    [name]
    (first (invocations-of name)))

  (defn last-invocation-of
    "Returns the list of arguments passed into the last invocation of the specified stub, nil if it was never invoked."
    [name]
    (last (invocations-of name)))


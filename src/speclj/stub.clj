(ns speclj.stub
  (:import [sun.plugin.dom.exception InvalidStateException])
  (:require [speclj.core :refer :all]
            [speclj.platform :refer [endl]]))

(declare ^:dynamic *stubbed-invocations*)

(defn with-stubs
  "Add this to describe/context blocks that use stubs.  It will setup a clean recording environment."
  []
  (around [it]
    (binding [*stubbed-invocations* (atom [])]
      (it))))

(defn- check-recording []
  (when-not (bound? #'*stubbed-invocations*)
    (throw (InvalidStateException. "Stub recoding not bound.  Please add (with-stubs) to the decribe/context."))))

(defn -record-invocation [name args]
  (check-recording)
  (let [args (if (= nil args) [] (vec args))]
    (swap! *stubbed-invocations* conj [name args])))

(defn- invoke-delegate [name delegate args]
  (try
    (apply delegate args)
    (catch clojure.lang.ArityException e
      (-fail (str "Stub " name " was invoked with " (.-actual e) " arguments, but the :invoke fn has a different arity")))))

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
        (throw (IllegalArgumentException. "stub's :invoke argument must be an ifn")))
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

(defmacro should-have-invoked
  "Checks for invocations of the specified stub.

  Options:
    :times - the number of times the stub should have been invoked. nil means at least once. (default: nil)
    :with - a list of arguments that the stubs should have been invoked with. Each call must have the same arguments.
      If not specified, anything goes.

  (should-have-invoked :foo {:with [1] :times 3})"
  ([name] `(should-have-invoked ~name {}))
  ([name options]
    `(let [name# ~name
           options# ~options
           invocations# (invocations-of name#)
           times# (:times options#)
           check-params?# (contains? options# :with)
           with# (:with options#)
           with# (if (nil? with#) [] with#)]
       (if (number? times#)
         (when-not (= times# (count invocations#))
           (-fail (str "Expected: " times# " invocation" (if (= 1 times#) "" "s") " of " name# endl "     got: " (count invocations#))))
         (when-not (seq invocations#)
           (-fail (str "Expected: an invocation of " name# endl "     got: " (count invocations#)))))
       (when check-params?#
         (doseq [invocation# (invocations-of name#)]
           (when-not (= with# invocation#)
             (-fail (str "Expected: invocation of " name# " with " (pr-str with#) endl "     got: " (pr-str invocation#)))))))))

(defmacro should-not-have-invoked
  "Asserts that the specified stub was never invoked.

  Same as: (should-have-invoked :foo {:times 0})"
  ([name] `(should-not-have-invoked ~name {}))
  ([name options]
    `(should-have-invoked ~name ~(assoc options :times 0))))

(defmacro should-invoke
  "Creates a stub, and binds it to the specified var, evaluates the body, and checks the invocations.

  (should-invoke reverse {:with [1 2 3] :return []} (reverse [1 2 3]))

  See stub and should-have-invoked for valid options."
  [var options & body]
  (when-not (map? options)
    (throw (IllegalArgumentException. "The second argument to should-invoke must be a map of options")))
  (let [var-name (str var)]
    `(let [options# ~options]
       (binding [*stubbed-invocations* (atom [])]
         (with-redefs [~var (stub ~var-name options#)]
           ~@body)
         (should-have-invoked ~var-name options#)))))

(defmacro should-not-invoke
  "Creates a stub, and binds it to the specified var, evaluates the body, and checks that is was NOT invoked.

  Same as: (should-invoke :foo {:times 0} ...)"
  [var options & body]
  `(should-invoke ~var ~(assoc options :times 0) ~@body))
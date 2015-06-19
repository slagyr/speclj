(ns speclj.running
  (:require [clojure.string :as str]
            [speclj.components :refer [reset-with]]
            [speclj.config :refer [*runner* active-reporters]]
            [speclj.platform :refer [pending? throwable secs-since current-time]]
            [speclj.reporting :refer [report-runs* report-run report-description*]]
            [speclj.results :refer [pass-result fail-result pending-result error-result]]
            [speclj.tags :refer [tags-for tag-sets-for pass-tag-filter? context-with-tags-seq]]))

(defn- eval-components [components]
  (doseq [component components] ((.-body component))))

(defn nested-fns [base fns]
  (if (seq fns)
    (partial (first fns) (nested-fns base (rest fns)))
    base))

(defn- eval-characteristic [befores body afters]
  (eval-components befores)
  (try
    (body)
    (finally
      (eval-components afters))))

(defn- reset-withs [withs]
  (doseq [with withs] (reset-with with)))

(defn- collect-components [getter description]
  (loop [description description components []]
    (if description
      (recur @(.-parent description) (concat (getter description) components))
      components)))

(defn- report-result [result-constructor characteristic start-time reporters failure]
  (let [present-args (filter identity [characteristic (secs-since start-time) failure])
        result (apply result-constructor present-args)]
    (report-run result reporters)
    result))

(defn- do-characteristic [characteristic reporters]
  (let [description @(.-parent characteristic)
        befores (collect-components #(deref (.-befores %)) description)
        afters (collect-components #(deref (.-afters %)) description)
        core-body (.-body characteristic)
        before-and-after-body (fn [] (eval-characteristic befores core-body afters))
        arounds (collect-components #(deref (.-arounds %)) description)
        full-body (nested-fns before-and-after-body (map #(.-body %) arounds))
        withs (collect-components #(deref (.-withs %)) description)
        start-time (current-time)]
    (try
      (do
        (full-body)
        (report-result pass-result characteristic start-time reporters nil))
      (catch #?(:clj  java.lang.Throwable
                :cljs :default)
             e
        (if (pending? e)
          (report-result pending-result characteristic start-time reporters e)
          (report-result fail-result characteristic start-time reporters e)))
      (finally
        (reset-withs withs)))))                             ;MDM - Possible clojure bug.  Inlining reset-withs results in compile error

(defn- do-characteristics [characteristics reporters]
  (doall
    (for [characteristic characteristics]
      (do-characteristic characteristic reporters))))

(declare do-description)

(defn- do-child-contexts [context results reporters]
  (loop [results results contexts @(.-children context)]
    (if (seq contexts)
      (recur (concat results (do-description (first contexts) reporters)) (rest contexts))
      (do
        (eval-components @(.-after-alls context))
        results))))

(defn- results-for-context [context reporters]
  (if (pass-tag-filter? (tags-for context))
    (do-characteristics @(.-charcteristics context) reporters)
    []))

#?(:clj
   (defn- with-withs-bound [description body]
     (let [withs (concat @(.-withs description) @(.-with-alls description))
           ns (the-ns (symbol (.-ns description)))
           with-mappings (reduce #(assoc %1 (ns-resolve ns (.-name %2)) %2) {} withs)]
       (with-bindings* with-mappings body)))

   :cljs
   (defn- with-withs-bound [description body]
     (let [withs (concat @(.-withs description) @(.-with-alls description))
           ns (str/replace (.-ns description) "-" "_")
           var-names (map #(str ns "." (name (.-name %))) withs)
           unique-names (map #(str ns "." (name (.-unique-name %))) withs)]
       (doseq [[n un] (partition 2 (interleave var-names unique-names))]
         (let [code (str n " = " un ";")]
           (js* "eval(~{})" code)))
       (try
         (body)
         (finally
           (doseq [[n] var-names]
             (let [code (str n " = null;")]
               (js* "eval(~{})" code))))))))

(defn- nested-results-for-context [description reporters]
  (let [results (results-for-context description reporters)]
    (do-child-contexts description results reporters)))

(defn- with-around-alls [description run-characteristics-fn]
  ((nested-fns run-characteristics-fn
               (map #(.-body %) @(.-around-alls description)))))

(defn do-description [description reporters]
  (let [tag-sets (tag-sets-for description)]
    (when (some pass-tag-filter? tag-sets)
      (report-description* reporters description)
      (with-withs-bound description
                        (fn []
                          (eval-components @(.-before-alls description))

                          (try
                            (with-around-alls
                              description
                              (partial nested-results-for-context description reporters))

                            (finally
                              (reset-withs @(.-with-alls description)))))))))

(defn process-compile-error [runner e]
  (let [error-result (error-result e)]
    (swap! (.-results runner) conj error-result)
    (report-run error-result (active-reporters))))

(defprotocol Runner
  (run-directories [this directories reporters])
  (submit-description [this description])
  (run-description [this description reporters])
  (run-and-report [this reporters]))


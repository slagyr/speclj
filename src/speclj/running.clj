(ns speclj.running
  (:use
    [speclj.exec :only (pass-result fail-result pending-result)]
    [speclj.reporting :only (report-runs report-run report-description)]
    [speclj.components :only (reset-with)]
    [speclj.util :only (secs-since)]
    [speclj.config :only (*runner* active-reporter)]
    [speclj.tags :only (tags-for tag-sets-for pass-tag-filter? context-with-tags-seq)])
  (:import
    [speclj SpecPending]
    [java.io File]))

(defn- eval-components [components]
  (doseq [component components] ((.body component))))

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
      (recur @(.parent description) (concat (getter description) components))
      components)))

(defn- report-result [result-constructor characteristic start-time reporter failure]
  (let [present-args (filter identity [characteristic (secs-since start-time) failure])
        result (apply result-constructor present-args)]
    (report-run result reporter)
    result))

(defn- do-characteristic [characteristic reporter]
  (let [description @(.parent characteristic)
        befores (collect-components #(deref (.befores %)) description)
        afters (collect-components #(deref (.afters %)) description)
        core-body (.body characteristic)
        before-and-after-body (fn [] (eval-characteristic befores core-body afters))
        arounds (collect-components #(deref (.arounds %)) description)
        full-body (nested-fns before-and-after-body (map #(.body %) arounds))
        withs (collect-components #(deref (.withs %)) description)
        start-time (System/nanoTime)]
    (try
      (do
        (full-body)
        (report-result pass-result characteristic start-time reporter nil))
      (catch SpecPending p
        (report-result pending-result characteristic start-time reporter p))
      (catch Exception e
        (report-result fail-result characteristic start-time reporter e))
      (finally
        (reset-withs withs))))) ;MDM - Possible clojure bug.  Inlining reset-withs results in compile error 

(defn- do-characteristics [characteristics reporter]
  (doall
    (for [characteristic characteristics]
      (do-characteristic characteristic reporter))))

(defn- withs-mapping [description]
  (let [withs (concat @(.withs description) @(.with-alls description))
        ns (.ns description)]
    (reduce #(assoc %1 (ns-resolve ns (.name %2)) %2) {} withs)))

(declare do-description)

(defn- do-child-contexts [context results reporter]
  (loop [results results contexts @(.children context)]
    (if (seq contexts)
      (recur (concat results (do-description (first contexts) reporter)) (rest contexts))
      (do
        (eval-components @(.after-alls context))
        results))))

(defn- results-for-context [context reporter]
  (if (pass-tag-filter? (tags-for context))
    (do-characteristics @(.charcteristics context) reporter)
    []))

(defn do-description [description reporter]
  (let [tag-sets (tag-sets-for description)]
    (when (some pass-tag-filter? tag-sets)
      (report-description reporter description)
      (eval-components @(.before-alls description))
      (with-bindings (withs-mapping description)
        (try
          (let [results (results-for-context description reporter)]
            (do-child-contexts description results reporter))
          (finally
            (reset-withs @(.with-alls description))))))))

(defprotocol Runner
  (run-directories [this directories reporter])
  (submit-description [this description])
  (run-description [this description reporter])
  (run-and-report [this reporter]))

(def clj-file-regex #".*\.clj")
(defn clj-files-in [& dirs]
  (let [files (reduce #(into %1 (file-seq (File. %2))) [] dirs)]
    (filter #(re-matches clj-file-regex (.getName %)) files)))



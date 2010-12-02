(ns speclj.running
  (:use
    [speclj.exec :only (pass-result fail-result)]
    [speclj.reporting :only (active-reporter report-runs report-pass report-fail report-description)]
    [speclj.components :only (reset-with)]
    [speclj.util :only (secs-since)])
  (:import
    [java.io File]))

(def default-runner (atom nil))
(declare *runner*)
(defn active-runner []
  (if (bound? #'*runner*)
    *runner*
    (if-let [runner @default-runner]
      runner
      (throw (Exception. "*runner* is unbound and no default value has been provided")))))

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

(defn- do-characteristic [characteristic reporter]
  (let [description @(.description characteristic)
        befores (collect-components #(deref (.befores %)) description)
        arounds (collect-components #(deref (.arounds %)) description)
        body (nested-fns (.body characteristic) (map #(.body %) arounds))
        afters (collect-components #(deref (.afters %)) description)
        withs (collect-components #(deref (.withs %)) description)
        start-time (System/nanoTime)]
    (try
      (eval-characteristic befores body afters)
      (let [result (pass-result characteristic (secs-since start-time))]
        (report-pass reporter result)
        result)
      (catch Exception e
        (let [result (fail-result characteristic (secs-since start-time) e)]
          (report-fail reporter result)
          result))
      (finally
        (reset-withs withs))))) ;MDM - Possible clojure bug.  Inlining reset-withs results in compile error 

(defn- do-characteristics [characteristics reporter]
  (doall
    (for [characteristic characteristics]
      (do-characteristic characteristic reporter))))

(defn- withs-mapping [description]
  (let [withs @(.withs description)
        ns (.ns description)]
    (reduce #(assoc %1 (ns-resolve ns (.name %2)) %2) {} withs)))

(defn do-description [description reporter]
  (report-description reporter description)
  (eval-components @(.before-alls description))
  (with-bindings (withs-mapping description)
    (let [results (do-characteristics @(.charcteristics description) reporter)]
      (loop [results results descriptions @(.children description)]
        (if (seq descriptions)
          (recur (concat results (do-description (first descriptions) reporter)) (rest descriptions))
          (do
            (eval-components @(.after-alls description))
            results))))))

(defprotocol Runner
  (run-directories [this directories reporter])
  (submit-description [this description])
  (run-description [this description reporter])
  (run-and-report [this reporter]))

(def clj-file-regex #".*\.clj")
(defn clj-files-in [& dirs]
  (let [files (reduce #(into %1 (file-seq (File. %2))) [] dirs)]
    (filter #(re-matches clj-file-regex (.getName %)) files)))



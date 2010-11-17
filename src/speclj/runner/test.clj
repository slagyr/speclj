(ns speclj.runner.test
  (:use
    [clojure.java.io]
    [clojure.contrib.find-namespaces :only (find-clojure-sources-in-dir read-file-ns-decl)]
    [clojure.set :only (union)]))

(defn- deps-from-libspec [prefix form]
  (cond (list? form) (apply union (map (fn [f] (deps-from-libspec
						(symbol (str (when prefix (str prefix "."))
							     (first form)))
						f))
				       (rest form)))
	(vector? form) (deps-from-libspec prefix (first form))
	(symbol? form) #{(symbol (str (when prefix (str prefix ".")) form))}
	(keyword? form) #{}
	:else (throw (IllegalArgumentException.
		      (pr-str "Unparsable namespace form:" form)))))

(defn- deps-from-ns-form [form]
  (when (and (list? form)
	     (contains? #{:use :require} (first form)))
    (apply union (map #(deps-from-libspec nil %) (rest form)))))

(defn deps-from-ns-decl
  "Given a (quoted) ns declaration, returns a set of symbols naming
  the dependencies of that namespace.  Handles :use and :require clauses."
  [decl]
  (apply union (map deps-from-ns-form decl)))

(defn graph "Returns a new, empty, dependency graph." []
  {:dependencies {}
   :dependents {}})

(defn- transitive
  "Recursively expands the set of dependency relationships starting
  at (get m x)"
  [m x]
  (reduce (fn [s k]
    (union s (transitive m k)))
    (get m x) (get m x)))

(defn dependencies
  "Returns the set of all things x depends on, directly or transitively."
  [graph x]
  (transitive (:dependencies graph) x))

(defn dependents
  "Returns the set of all things which depend upon x, directly or
  transitively."
  [graph x]
  (transitive (:dependents graph) x))

(defn depends?
  "True if x is directly or transitively dependent on y."
  [graph x y]
  (contains? (dependencies graph x) y))

(defn dependent
  "True if y is a dependent of x."
  [graph x y]
  (contains? (dependents graph x) y))

(defn- add-relationship [graph key x y]
  (update-in graph [key x] union #{y}))

(defn depend
  "Adds to the dependency graph that x depends on deps.  Forbids
  circular dependencies."
  ([graph x] graph)
  ([graph x dep]
    {:pre [(not (depends? graph dep x))]}
    (-> graph
      (add-relationship :dependencies x dep)
      (add-relationship :dependents dep x)))
  ([graph x dep & more]
    (reduce (fn [g d] (depend g x d))
      graph (cons dep more))))

(defn- remove-from-map [amap x]
  (reduce (fn [m [k vs]]
    (assoc m k (disj vs x)))
    {} (dissoc amap x)))

(defn remove-all
  "Removes all references to x in the dependency graph."
  ([graph] graph)
  ([graph x]
    (assoc graph
      :dependencies (remove-from-map (:dependencies graph) x)
      :dependents (remove-from-map (:dependents graph) x)))
  ([graph x & more]
    (reduce remove-all
      graph (cons x more))))

(defn remove-key
  "Removes the key x from the dependency graph without removing x as a
  depedency of other keys."
  ([graph] graph)
  ([graph x]
    (assoc graph
      :dependencies (dissoc (:dependencies graph) x)))
  ([graph x & more]
    (reduce remove-key
      graph (cons x more))))

(defn- find-sources
  [dirs]
  {:pre [(every? (fn [d] (instance? java.io.File d)) dirs)]}
  (mapcat find-clojure-sources-in-dir dirs))

(defn- newer-sources [dirs timestamp]
  (filter #(> (.lastModified %) timestamp) (find-sources dirs)))

(defn- newer-namespace-decls [dirs timestamp]
  (remove nil? (map read-file-ns-decl (newer-sources dirs timestamp))))

(defn- add-to-dep-graph [dep-graph namespace-decls]
  (reduce (fn [g decl]
	    (let [nn (second decl)
		  deps (deps-from-ns-decl decl)]
	      (apply depend g nn deps)))
	  dep-graph namespace-decls))

(defn- remove-from-dep-graph [dep-graph new-decls]
  (apply remove-key dep-graph (map second new-decls)))

(defn- update-dependency-graph [dep-graph new-decls]
  (-> dep-graph
      (remove-from-dep-graph new-decls)
      (add-to-dep-graph new-decls)))

(defn- affected-namespaces [changed-namespaces old-dependency-graph]
  (apply union (set changed-namespaces) (map #(dependents old-dependency-graph %)
					     changed-namespaces)))

(defn tracker [dirs initial-timestamp]
  "Returns a no-arg function which, when called, returns a set of
  namespaces that need to be reloaded, based on file modification
  timestamps and the graph of namespace dependencies."
  {:pre [(integer? initial-timestamp)
	 (every? (fn [f] (instance? java.io.File f)) dirs)]}
  (let [timestamp (atom initial-timestamp)
	dependency-graph (atom (graph))]
    (fn []
      (let [then @timestamp
	    now (System/currentTimeMillis)
	    new-decls (newer-namespace-decls dirs then)]
	(when (seq new-decls)
	  (let [new-names (map second new-decls)
		affected-names (affected-namespaces new-names @dependency-graph)]
	    (reset! timestamp now)
	    (swap! dependency-graph update-dependency-graph new-decls)
(println "@dependency-graph: " @dependency-graph)      
	    affected-names))))))

(println ((tracker [(file "/Users/micahmartin/Projects/clojure/speclj/test") (file "/Users/micahmartin/Projects/clojure/speclj/src")] 0)))

{:dependencies
  {speclj.core-test #{speclj.core},
   speclj.should-test #{speclj.core speclj.test-help speclj.util},
   speclj.running #{speclj.components speclj.reporting speclj.exec},
   speclj.core #{speclj.running speclj.components speclj.util},
   speclj.runner.vigilant-runner-test #{speclj.core speclj.runner.vigilant-runner clojure.java.io},
   speclj.runner.test #{clojure.contrib.find-namespaces clojure.set clojure.java.io},
   speclj.runner.vigilant-runner #{speclj.running speclj.util},
   speclj.tree-test #{speclj.core speclj.runner.tree},
   speclj.reporting #{speclj.exec},
   speclj.runner #{speclj.running speclj.reporting},
   speclj.running-test #{speclj.running speclj.core speclj.reporting},
   speclj.reporting-test #{speclj.core speclj.reporting}},
:dependents
  {speclj.running #{speclj.core speclj.runner.vigilant-runner speclj.runner speclj.running-test},
   speclj.core #{speclj.core-test speclj.should-test speclj.runner.vigilant-runner-test speclj.tree-test speclj.running-test speclj.reporting-test},
   speclj.runner.tree #{speclj.tree-test},
   speclj.runner.vigilant-runner #{speclj.runner.vigilant-runner-test},
   speclj.components #{speclj.running speclj.core},
   clojure.contrib.find-namespaces #{speclj.runner.test},
   speclj.reporting #{speclj.running speclj.runner speclj.running-test speclj.reporting-test},
   speclj.exec #{speclj.running speclj.reporting}, speclj.test-help #{speclj.should-test},
   clojure.set #{speclj.runner.test},
   speclj.util #{speclj.should-test speclj.core speclj.runner.vigilant-runner},
   clojure.java.io #{speclj.runner.vigilant-runner-test speclj.runner.test}}}

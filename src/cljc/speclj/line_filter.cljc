(ns speclj.line-filter
  (:require [clojure.string :as str]
            [speclj.components :as components]
            [speclj.config :refer [*line-targets*]]))

(def ^:dynamic *chosen-characteristics* nil)

;; Descriptions on the ancestor chain of at least one chosen characteristic.
;; When line-filter is active, these are the only descriptions we let the
;; runner traverse — so reporters don't print headers for unrelated files.
(def ^:dynamic *chosen-descriptions* nil)

;; Bound (to an atom) by speclj.cli/do-specs so unmatched file:line targets can
;; surface as warnings + non-zero exit after the run. nil means nobody cares.
(def ^:dynamic *run-unmatched* nil)

(defn- parse-int [s]
  #?(:clj  (Long/parseLong s)
     :cljr (System.Int64/Parse s)
     :cljs (js/parseInt s 10)))

(defn split-line-spec
  "Splits \"path:line\" → [path line-int]. Returns [s nil] when s has no
   numeric :N suffix. Preserves Windows drive letters like \"C:\\\\foo.clj\"
   by requiring the matched path to be longer than one char and to not end
   with a colon (which would mean a bare drive letter or \"foo::N\")."
  [s]
  (if-let [[_ path line-str] (re-matches #"^(.+):(\d+)$" s)]
    (if (and (> (count path) 1)
             (not (str/ends-with? path ":")))
      [path (parse-int line-str)]
      [s nil])
    [s nil]))

(defn- path-matches?
  "True when user-path and component-file refer to the same file. Either may
   be absolute or relative; we accept matches in both directions so that
   `speclj spec/foo.clj:42` matches components whose :file is the reader's
   absolute path *or* a classpath-relative form."
  [user-path component-file]
  (when (and user-path component-file)
    (let [u (str user-path)
          c (str component-file)]
      (or (= u c)
          (str/ends-with? c u)
          (str/ends-with? u c)))))

(defn- loc-of [component] (.-loc component))
(defn- line-of [component] (:line (loc-of component)))
(defn- file-of [component] (:file (loc-of component)))

(defn- walk-components
  "Depth-first seq over every Description and Characteristic reachable from
   the given roots."
  [descriptions]
  (let [expand (fn expand [c]
                 (lazy-seq
                   (cons c
                         (when (components/is-description? c)
                           (mapcat expand
                                   (concat @(.-characteristics c)
                                           @(.-children c)))))))]
    (mapcat expand descriptions)))

(defn- descendants-of [description]
  (->> (walk-components [description])
       (filter components/is-characteristic?)))

(defn- best-match
  "Among file-components (already filtered to the target file), returns the
   component whose :line is the greatest value ≤ target-line. A
   Characteristic wins ties against a Description at the same line, since
   the innermost matching scope is the more specific pick."
  [file-components target-line]
  (let [eligible (filter #(and (line-of %) (<= (line-of %) target-line))
                         file-components)]
    (when (seq eligible)
      (let [max-line (apply max (map line-of eligible))
            at-max   (filter #(= (line-of %) max-line) eligible)]
        (or (first (filter components/is-characteristic? at-max))
            (first at-max))))))

(defn resolve-targets
  "Walks the description tree once and resolves each [user-path line-num]
   target into a set of chosen Characteristic instances plus a list of
   unmatched target strings (targets whose file isn't loaded).

   Returns {:chosen #{characteristics...} :unmatched [\"path:line\" ...]}"
  [descriptions line-targets]
  (let [all (walk-components descriptions)]
    (reduce
      (fn [acc [user-path target-line]]
        (let [file-comps (filter #(path-matches? user-path (file-of %)) all)]
          (if (empty? file-comps)
            (update acc :unmatched conj (str user-path ":" target-line))
            (if-let [winner (best-match file-comps target-line)]
              (cond
                (components/is-characteristic? winner)
                (update acc :chosen conj winner)

                (components/is-description? winner)
                (update acc :chosen into (descendants-of winner))

                :else acc)

              ;; No component at or before target-line → run all characteristics in file.
              (update acc :chosen into
                      (filter components/is-characteristic? file-comps))))))
      {:chosen #{} :unmatched []}
      line-targets)))

(defn active?
  "True once *chosen-characteristics* has been bound (even to an empty set).
   Callers bind this explicitly when `*line-targets*` is non-empty."
  []
  (some? *chosen-characteristics*))

(defn filtered-file?
  "True when this component's :file is named by some entry in *line-targets*.
   Untargeted files run through normal focus/tag logic; only files with an
   active line-target get narrowed."
  [component]
  (let [f (file-of component)]
    (boolean
      (and f
           (seq *line-targets*)
           (some (fn [user-path] (path-matches? user-path f))
                 (keys *line-targets*))))))

(defn- ancestors-of
  "Walks up each chosen characteristic's parent chain and returns the set of
   Description instances we pass through."
  [chosen]
  (reduce
    (fn [acc characteristic]
      (loop [desc @(.-parent characteristic)
             acc  acc]
        (if desc
          (recur @(.-parent desc) (conj acc desc))
          acc)))
    #{}
    chosen))

(defn pass-line-filter?
  "Only call this for components in `filtered-file?` files; untargeted files
   should be gated by the normal focus/tag logic instead. Characteristics
   pass iff they're in *chosen-characteristics*; Descriptions pass iff they
   sit on the ancestor chain of a chosen characteristic."
  [component]
  (cond
    (components/is-description? component)
    (contains? *chosen-descriptions* component)
    (components/is-characteristic? component)
    (contains? *chosen-characteristics* component)
    :else true))

(defn with-chosen
  "If `*line-targets*` is non-empty, resolves them against `descriptions`,
   records any unmatched targets into `*run-unmatched*` (when bound), and
   invokes body-fn with `*chosen-characteristics*` and
   `*chosen-descriptions*` bound. Otherwise just invokes body-fn."
  [descriptions body-fn]
  (if (seq *line-targets*)
    (let [{:keys [chosen unmatched]} (resolve-targets descriptions *line-targets*)]
      (when (and *run-unmatched* (seq unmatched))
        (swap! *run-unmatched* into unmatched))
      (binding [*chosen-characteristics* chosen
                *chosen-descriptions*    (ancestors-of chosen)]
        (body-fn)))
    (body-fn)))

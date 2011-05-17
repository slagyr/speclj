(ns speclj.tags
  (:use
    [speclj.config :only (*tag-filter*)]
    [clojure.set :only (union intersection)]))

(defn pass-includes? [includes tags]
  (if (empty? includes)
    true
    (= includes (intersection includes (set tags)))))

(defn pass-excludes? [excludes tags]
  (if (empty? excludes)
    true
    (not (some
      #(contains? excludes %)
      tags))))

(defn pass-tag-filter?
  ([tags] (pass-tag-filter? *tag-filter* tags))
  ([filter tags]
    (and
      (pass-includes? (:includes filter) tags)
      (pass-excludes? (:excludes filter) tags))))

(defn tags-for [context]
  (if context
    (union (tags-for @(.parent context)) @(.tags context))
    #{}))

(defn tag-sets-for [context]
  (let [context-seq (tree-seq #(not (nil? %)) #(deref (.children %)) context)]
    (map tags-for context-seq)))

(defn context-with-tags-seq [context]
  (let [context-seq (tree-seq #(not (nil? %)) #(deref (.children %)) context)]
    (map #(hash-map :context % :tag-set (tags-for %)))))

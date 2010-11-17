(ns speclj.runner.tree)

(deftype TreeNode [children payload])

(defn new-tree [payload]
  (TreeNode. (atom nil) payload))

(defn- new-node [payload]
  (TreeNode. (atom nil) payload))

(defn add-to-node [parent & children-payloads]
  (doseq [child-payload children-payloads]
    (let [child-list (.children parent)
          new-child-list (conj (vec @child-list) (new-node child-payload))]
      (swap! child-list (fn [_] new-child-list)))))
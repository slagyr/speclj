(ns speclj.tree-test
  (:use
    [speclj.core]
    [speclj.runner.tree]))

(defn build-family-tree)

(describe "Tree"
  (with root (new-tree "root"))

  (it "Has a root"
    (should-not= nil @root))

  (it "Root as no children"
    (should= nil @(.children @root)))

  (it "Remembers it's data"
    (should= "root" (.payload @root)))

  (it "can add child to root"
    (add-to-node @root "child")
    (should= 1 (count @(.children @root)))
    (should= "child" (.payload (first @(.children @root)))))

  (it "can add grand child"
    (add-to-node @root "child")
    (let [child (first @(.children @root))]
      (add-to-node child "grand-child")
      (should= 1 (count @(.children child)))
      (should= "grand-child" (.payload (first @(.children child))))))

  (it "can find nodes in a tree"
    (build-family-tree @root))

  )


(conclude-single-file-run)
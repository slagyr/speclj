;; Partial compatibility with API for clojure.data.xml, which depends
;; on Java.xml.* - Uses browser/phantomjs DOM to construct XML
(ns speclj.report.junit.xml
  (:require [clojure.string :as string]))

(declare content->child)

(defn- content->children
  "Create a DocumentFragment containing DOM representations of each child."
  [content]
  (let [fragment (js/document.createDocumentFragment)]
    (doseq [x content]
      (when-not (nil? x)
        (.appendChild fragment (content->child x))))
    fragment))

(defn- ->str [x]
  (cond
    (instance? js/Date x) (.toISOString x)
    (satisfies? INamed x) (name x)
    :else (str x)))

(defn- text
  "Create a Text node suitable for appending to a DocumentFragment."
  [x]
  (js/document.createTextNode (->str x)))

(def ^:private dom-node?
  (partial instance? js/Node))

(defn- content->child
  "Returns a child DOM node:
  - Self, if already a DOM node
  - DocumentFragment if sequential
  - Otherwise a Text node containing the string representation of x"
  [x]
  (cond
    (dom-node? x)  x
    (sequential? x)    (content->children x)
    :else              (text x)))

(defn- ->attr-val [attr-name x]
  (if (instance? js/Boolean x)
      (when x attr-name)
      (->str x)))

(defn element
  "Create a DOM element. Used in place of clojure.data.xml/element"
  [tag & [attrs & content]]
  (let [el (js/document.createElement (name tag))
        content- (remove nil? content)]
    ;; Set attributes
    (doseq [[k v] attrs
            :let [attr-name (->str k)]]
      (when-let [attr-val (->attr-val k v)]
        (.setAttribute el attr-name attr-val)))

    ;; Set content
    (.appendChild el (content->child content))

    el))

(defn emit-str
  "Emit XML string for an element. Used in place of clojure.data.xml/emit-str"
  [el]
  (string/trim
    (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
         (.-outerHTML el))))

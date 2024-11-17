(ns speclj.mmargs
  (:require [clojure.java.data :as data]
            [speclj.args :as args]))

(defn- add-entry [m key value]
  (let [value (data/from-java value)]
    (assoc m (keyword key) value)))

(defn- -parse [mmargs args]
  (->> (.parse mmargs (into-array String args))
       (reduce-kv add-entry {})))

(deftype Arguments [mmargs]
  args/Arguments
  (add-multi-parameter [this name description]
    (.addMultiParameter mmargs name description)
    this)
  (add-switch-option [this short-name full-name description]
    (.addSwitchOption mmargs short-name full-name description)
    this)
  (add-value-option [this short-name full-name value-description description]
    (.addValueOption mmargs short-name full-name value-description description)
    this)
  (add-multi-option [this short-name full-name value-description description]
    (.addMultiOption mmargs short-name full-name value-description description)
    this)
  (add-parameter [this name description]
    (.addParameter mmargs name description)
    this)
  (add-optional-parameter [this name description]
    (.addOptionalParameter mmargs name description)
    this)
  (parse [_this args] (-parse mmargs args))
  (arg-string [_this] (.argString mmargs))
  (parameters-string [_this] (.parametersString mmargs))
  (options-string [_this] (.optionsString mmargs)))

(defn ->Arguments []
  (Arguments. (mmargs.Arguments.)))

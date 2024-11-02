(ns speclj.mmargs
  (:require [speclj.args :as args]))

(defn- add-entry [m entry]
  (assoc m (keyword (.getKey entry)) (.getValue entry)))

(defn- parse-args [mmargs args]
  (->> (.parse mmargs (into-array String args))
       (reduce add-entry {})))

(deftype Arguments [mmargs]
  args/Arguments
  (add-multi-parameter [_this name description]
    (.addMultiParameter mmargs name description))
  (add-switch-option [_this short-name full-name description]
    (.addSwitchOption mmargs short-name full-name description))
  (add-value-option [_this short-name full-name value-description description]
    (.addValueOption mmargs short-name full-name value-description description))
  (add-multi-option [_this short-name full-name value-description description]
    (.addMultiOption mmargs short-name full-name value-description description))
  (parse [_this args] (parse-args mmargs args))
  (arg-string [_this] (.argString mmargs))
  (parameters-string [_this] (.parametersString mmargs))
  (options-string [_this] (.optionsString mmargs)))

(defn ->Arguments []
  (Arguments. (mmargs.Arguments.)))

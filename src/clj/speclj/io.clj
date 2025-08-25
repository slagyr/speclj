(ns speclj.io
  (:require [clojure.java.io :as io])
  (:import (clojure.lang LineNumberingPushbackReader)
           (java.io File StringReader StringWriter)))

(defn as-file
  ([parent child] (io/file parent child))
  ([file] (io/as-file file)))

(defn canonical-file [file] (.getCanonicalFile file))
(defn canonical-path [file] (.getCanonicalPath file))
(defn hidden? [file] (.isHidden file))
(defn file-name [file] (.getName file))
(defn full-name [file] (.getAbsolutePath file))
(defn exists? [file] (.exists file))
(defn parent-file [file] (.getParentFile file))
(defn delete [file] (io/delete-file file))

(defn ->LineNumberingReader [reader] (LineNumberingPushbackReader. reader))
(defn ->StringReader [s] (StringReader. s))
(defn ->StringWriter [] (StringWriter.))

(defn copy [input output] (io/copy input output))
(defn make-parents [f] (io/make-parents f))

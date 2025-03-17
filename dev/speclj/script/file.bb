(ns speclj.script.file
  (:require [babashka.fs :as fs]
            [speclj.script.core :as script]))

(defonce impl (atom nil))
(defmulti get-permissions (fn [_path] @impl))
(defmulti set-permissions (fn [_path _permissions] @impl))
(defmulti download-to (fn [_url _path] @impl))
(defmulti delete (fn [_path] @impl))
(defmulti sudo-execute (fn [_path] @impl))
(defmulti exists? (fn [_path] @impl))

;region Real

(defmethod exists? :default [path]
  (fs/exists? path))

(defmethod get-permissions :default [path]
  (fs/posix-file-permissions path))

(defmethod set-permissions :default [path permissions]
  (fs/set-posix-file-permissions path permissions))

(defmethod download-to :default [url path]
  (script/sh "curl" "-o" path url))

(defmethod delete :default [path]
  (fs/delete path))

(defmethod sudo-execute :default [path]
  (script/sh "sudo" (str "./" path)))

;endregion
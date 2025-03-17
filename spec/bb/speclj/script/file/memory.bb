(ns speclj.script.file.memory
  (:require [speclj.script.file :as file])
  (:import (java.nio.file NoSuchFileException)
           (java.nio.file.attribute PosixFilePermission)))

(defonce files (atom {}))
(defn clear! [] (reset! files {}))

(defmacro assert-file [path]
  `(let [path# ~path]
     (when-not (file/exists? path#)
       (throw (java.nio.file.NoSuchFileException. path#)))))

(def executable-permissions
  [PosixFilePermission/OWNER_EXECUTE
   PosixFilePermission/OTHERS_EXECUTE
   PosixFilePermission/GROUP_EXECUTE])

(defn- executable? [path]
  (when-let [file (get @files path)]
    (every? (:permissions file) executable-permissions)))

(defmethod file/exists? :memory [path]
  (contains? @files path))

(defmethod file/get-permissions :memory [path]
  (assert-file path)
  (:permissions (get @files path)))

(defmethod file/set-permissions :memory [path permissions]
  (assert-file path)
  (swap! files assoc-in [path :permissions] permissions))

(def default-file-permissions
  #{PosixFilePermission/OWNER_READ
    PosixFilePermission/OWNER_WRITE
    PosixFilePermission/OTHERS_READ
    PosixFilePermission/GROUP_READ})

(defmethod file/download-to :memory [url path]
  (swap! files assoc-in [path :permissions] default-file-permissions))

(defmethod file/delete :memory [path]
  (assert-file path)
  (swap! files dissoc path)
  nil)

(defmethod file/sudo-execute :memory [path]
  (if (executable? path)
    {:exit 0 :out "the-output" :err ""}
    {:exit 1 :out "" :err (str "sudo: ./" path ": command not found\n")}))
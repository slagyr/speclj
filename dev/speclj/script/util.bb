(ns speclj.script.util
  (:require [clojure.string :as str]
            [speclj.script.file :as file]
            [speclj.script.core :as script])
  (:import (java.nio.file.attribute PosixFilePermission)))

(defn as-executable [path]
  (let [permissions (-> (file/get-permissions path)
                        (conj PosixFilePermission/OWNER_EXECUTE
                              PosixFilePermission/GROUP_EXECUTE
                              PosixFilePermission/OTHERS_EXECUTE))]
    (file/set-permissions path permissions)))

(defn install!
  "Downloads and executes a script with sudo permissions,
   then deletes the script."
  [url]
  (let [filename (last (str/split url #"/"))]
    (file/download-to url filename)
    (as-executable filename)
    (let [result (file/sudo-execute filename)]
      (file/delete filename)
      result)))

(defn dotnet-tool-install
  "Installs a dotnet tool"
  [name version]
  (script/sh "dotnet" "tool" "install" "--global" name "--version" version))

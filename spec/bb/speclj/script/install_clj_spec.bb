(ns speclj.script.install-clj-spec
  (:require [babashka.fs :as fs]
            [speclj.core :refer :all]
            [speclj.script.file :as file]
            [speclj.script.install-clj :as sut]
            [speclj.script.spec-helper :as bb-helper])
  (:import (java.nio.file.attribute PosixFilePermission)))

(describe "Install Clojure"
  (with-stubs)
  (bb-helper/stub-system-exit)
  (bb-helper/stub-shell)

  (context "memory"
    (bb-helper/with-memory-files)
  
    (it "assigns execution permissions"
      (with-redefs [file/delete (stub :file/delete)]
        (sut/-main)
        (let [permissions (file/get-permissions "linux-install-1.11.1.1119.sh")]
          (should= 7 (count permissions))
          (should-contain PosixFilePermission/OTHERS_READ permissions)
          (should-contain PosixFilePermission/OTHERS_EXECUTE permissions)
          (should-contain PosixFilePermission/GROUP_READ permissions)
          (should-contain PosixFilePermission/GROUP_EXECUTE permissions)
          (should-contain PosixFilePermission/OWNER_READ permissions)
          (should-contain PosixFilePermission/OWNER_WRITE permissions)
          (should-contain PosixFilePermission/OWNER_EXECUTE permissions))))
    
    (it "executes the install script successfully"
      (sut/-main)
      (should-not-have-invoked :script/system-exit))
    
    (it "deletes the file"
      (sut/-main)
      (should-not-be file/exists? "linux-install-1.11.1.1119.sh"))
    )
    
  
  (context "shell"
    (redefs-around [fs/posix-file-permissions     (constantly #{})
                    fs/set-posix-file-permissions (fn [& _])
                    fs/delete                     (fn [& _])])
    
    (it "downloads installation from the internet to a self-named file"
      (sut/-main)
      (let [url  "https://download.clojure.org/install/linux-install-1.11.1.1119.sh"
            file "linux-install-1.11.1.1119.sh"]
        (should-have-invoked :shell/sh {:with ["curl" "-o" file url]})))
    
    (it "sudo executes the install script"
      (sut/-main)
      (should-have-invoked :shell/sh {:with ["sudo" "./linux-install-1.11.1.1119.sh"]}))
    )
  )

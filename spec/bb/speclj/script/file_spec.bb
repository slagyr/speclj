(ns speclj.script.file-spec
  (:require [babashka.fs :as fs]
            [clojure.java.shell :as shell]
            [speclj.core :refer :all]
            [speclj.script.spec-helper :as bb-helper]
            [speclj.script.file :as sut]))

(describe "File"
  (with-stubs)
  (bb-helper/stub-system-exit)
  (bb-helper/stub-shell)

  (redefs-around [fs/delete                     (stub :fs/delete)
                  fs/exists?                    (stub :fs/exists?)
                  fs/posix-file-permissions     (stub :fs/posix-file-permissions)
                  fs/set-posix-file-permissions (stub :fs/set-posix-file-permissions)])

  (it "exists?"
    (sut/exists? "blah")
    (should-have-invoked :fs/exists? {:with ["blah"]}))
          
  (it "get-permissions"
    (sut/get-permissions "blah")
    (should-have-invoked :fs/posix-file-permissions {:with ["blah"]}))
          
  (it "set-permissions"
    (sut/set-permissions "blah" #{:the-permissions})
    (should-have-invoked :fs/set-posix-file-permissions {:with ["blah" #{:the-permissions}]}))

  (it "download-to - success"
    (sut/download-to "the-url" "the-path")
    (should-have-invoked :shell/sh {:with ["curl" "-o" "the-path" "the-url"]}))
  
  (it "download-to - failure"
    (with-redefs [shell/sh (constantly {:exit 3})]
      (sut/download-to "the-url" "the-path")
      (should-have-invoked :script/system-exit {:with [3]})))

  (it "delete"
    (sut/delete "blah")
    (should-have-invoked :fs/delete {:with ["blah"]}))

  (it "sudo-execute success"
    (sut/sudo-execute "blah")
    (should-have-invoked :shell/sh {:with ["sudo" "./blah"]}))

  (it "sudo-execute failure"
    (with-redefs [shell/sh (constantly {:exit 5})]
      (sut/sudo-execute "blah")
      (should-have-invoked :script/system-exit {:with [5]})))
  )
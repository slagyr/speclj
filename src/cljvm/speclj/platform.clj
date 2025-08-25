(ns speclj.platform
  (:refer-clojure :rename {load-file core-load-file})
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.namespace.find :as find]))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
   http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing
   https://github.com/nberger/more-macro-musings"
  [then else]
  (if (:ns &env) then else))

(defmacro try-catch-anything
  "Tries forms up until the last form, which is expected to be a `catch` form,
  except its type is missing; instead, `:default` is used in ClojureScript and
  `java.lang.Throwable` is used in Clojure JVM."
  [& forms]
  (let [body         (butlast forms)
        catch-form   (last forms)
        [catch-sym binding & catch-forms] (if (sequential? catch-form) catch-form [nil nil nil])
        catch-valid? (and (= 'catch catch-sym) (symbol? binding))]
    (if catch-valid?
      `(if-cljs
         (try ~@body
              (catch :default ~binding ~@catch-forms))
         (try ~@body
              (catch java.lang.Throwable ~binding ~@catch-forms)))
      `(throw (ex-info "Invalid catch form" {:catch '~catch-form})))))

(def endl (System/getProperty "line.separator"))
(def file-separator (System/getProperty "file.separator"))
(def source-file-regex #".*\.clj(c)?")
(def find-platform find/clj)

(defn re? [obj] (instance? java.util.regex.Pattern obj))

(def throwable Throwable)
(def exception java.lang.Exception)

(defn- classname->filename [classname]
  (let [root-name (first (str/split classname #"\$"))]
    (str
      (str/replace root-name "." file-separator)
      ".clj")))

(declare ^:dynamic *bound-by-should-invoke*)

(defn bound-by-should-invoke? []
  (and (bound? #'*bound-by-should-invoke*)
       *bound-by-should-invoke*))

(defn difference-greater-than-delta? [expected actual delta]
  (> (abs (- (bigdec expected) (bigdec actual)))
     (abs (bigdec delta))))

(defn error-message [e] (.getMessage e))
(defn error-str [e] (str e))
(defn stack-trace [e] (seq (.getStackTrace e)))
(defn cause [e] (.getCause e))
(defn print-stack-trace [e]
  (.printStackTrace e (java.io.PrintWriter. *out* true)))

(defn failure-source [failure]
  (let [source    (nth (.getStackTrace failure) 0)
        classname (.getClassName source)
        filename  (classname->filename classname)
        line-no   (.getLineNumber source)]
    (if-let [url (io/resource filename)]
      {:file (io/as-file url) :line line-no}
      {:file filename :line line-no})))

(defn failure-source-str [exception]
  (let [{:keys [file line]} (failure-source exception)]
    (str file ":" line)))

(defn elide-level? [stack-element]
  (let [classname (.getClassName stack-element)]
    (or
      (.startsWith classname "clojure.")
      (.startsWith classname "speclj.")
      (.startsWith classname "java."))))

(defn type-name [t] (.getName t))

(defn current-date [] (java.util.Date.))
(def seconds-format (java.text.DecimalFormat. "0.00000"))
(defn format-seconds [secs] (.format seconds-format secs))
(defn current-time [] (System/nanoTime))
(defn current-millis [] (System/currentTimeMillis))
(defn secs-since [start] (/ (double (- (System/nanoTime) start)) 1000000000.0))

(defn dynamically-invoke [ns-name fn-name]
  (let [ns-sym (symbol ns-name)
        fn-sym (symbol (str ns-name "/" fn-name))
        expr   `(do (require '~ns-sym) (~fn-sym))]
    (eval expr)))

(defn read-in [] (.read *in*))
(defn exit [code] (System/exit code))

(defn load-file [file] (core-load-file file))

(defn get-name [ns] (.name ns))
(defn get-bytes [s] (seq (.getBytes s)))

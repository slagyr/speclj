(ns speclj.run.standard
  (:require #?@(:cljs    [[speclj.report.progress]
                          [speclj.components :as components]]
                :default [[speclj.freshener :as fresh]
                          [speclj.io :as io]])
            [speclj.config :as config]
            [speclj.platform :as platform]
            [speclj.reporting :as reporting]
            [speclj.results :as results]
            [speclj.running :as running]
            [speclj.tags :as tags]))

#?(:cljs
   (do
     (def ^:export armed false)
     (def counter (atom 0))
     (defn ^:export arm [] (set! armed true))
     (defn ^:export disarm [] (set! armed false))))

#?(:cljs
   (defn- load-spec [_spec-file]
     (js/alert "speclj.run.standard.load-spec:  I don't know how to do this."))

   :bb
   (defn- load-spec [spec-file]
     (load-file spec-file))

   :default
   (do
     (defn- file->pushback-reader [file]
       (-> (io/canonical-path file)
           slurp
           io/->StringReader
           io/->LineNumberingReader))

     (defn- load-spec [spec-file]
       (let [rdr  (file->pushback-reader spec-file)
             path (io/full-name spec-file)]
         (platform/compiler-load rdr path)))
     ))

(defn- try-load-spec [runner file]
  (try
    (load-spec file)
    (catch #?(:clj Throwable :cljr Exception :cljs :default) e
      (running/process-compile-error runner e))))

;; TODO [BAC]: cljs breaks StandardRunner interface.
;;   Is num necessary for cljs?
;;   Should clj have num as well?
(deftype StandardRunner [#?(:cljs num) descriptions results]
  running/Runner
  #?(:cljs
     (run-directories [_this _directories _reporters]
                      (js/alert "StandardRunner.run-directories:  I don't know how to do this."))
     :default
     (run-directories [this directories reporters]
       (let [files (->> (map io/as-file directories)
                        (apply fresh/clj-files-in)
                        (sort-by io/full-name))]
         (binding [config/*runner*    this
                   config/*reporters* reporters]
           (run! #(try-load-spec this %) files)))
       (running/run-and-report this reporters)
       (results/fail-count @results))
     )

  (-get-descriptions [_this] @descriptions)

  (submit-description [_this description]
    (swap! descriptions conj description))

  (-filter-descriptions [_this namespaces]
    (swap! descriptions running/descriptions-with-namespaces namespaces))

  (run-description [_this description reporters]
    (let [run-results (running/do-description description reporters)]
      (swap! results into run-results)))

  (run-and-report [this reporters]
    (doseq [description (running/filter-focused @descriptions)]
      (running/run-description this description reporters))
    (reporting/report-runs* reporters @results)))

#?(:cljs
   (extend-protocol IPrintWithWriter
     StandardRunner
     (-pr-writer [x writer opts]
       (-write writer (str "#<speclj.run.standard.StandardRunner(num: " (.-num x) ", descriptions: "))
       (-pr-writer @(.-descriptions x) writer opts)
       (-write writer ")>"))
     components/Description
     (-pr-writer [x writer _opts]
       (-write writer (str "#<speclj.component.Description(name: " (.-name x) ")>")))))

(defn ^:export new-standard-runner []
  (StandardRunner. #?(:cljs (swap! counter inc)) (atom []) (atom [])))

(reset! config/default-runner-fn new-standard-runner)
(reset! config/default-runner (new-standard-runner))

(defn- execute-active-runner []
  (when-let [filter-msg (tags/describe-filter)]
    (reporting/report-message* (config/active-reporters) filter-msg))
  (running/run-and-report (config/active-runner) (config/active-reporters)))

(defn config-with-defaults [configurations]
  (as-> (apply hash-map configurations) $
        (update-keys $ keyword)
        (merge (dissoc config/default-config :runner) $)))

#?(:cljs
   (defn ^:export run-specs [& configurations]
     "If evaluated outside the context of a spec run, it will run all the specs that have been evaluated using the default
      runner and reporter.  A call to this function is typically placed at the end of a spec file so that all the specs
      are evaluated by evaluation the file as a script.  Optional configuration parameters may be passed in:

      (run-specs :stacktrace true :color false :reporters [\"documentation\"])"
     (when armed
       (config/with-config
         (config-with-defaults configurations)
         (fn []
           (execute-active-runner)
           (results/fail-count @(.-results (config/active-runner)))))))

   :default
   (defn run-specs [& configurations]
     (when (identical? (config/active-runner) @config/default-runner) ; Solo file run?
       (let [config (config-with-defaults configurations)]
         (with-bindings (config/config-mappings config)
           (execute-active-runner)
           (reset! config/default-runner (@config/default-runner-fn))))))
   )

(ns speclj.clj-args
  (:require [clojure.string :as str]
            [speclj.args :as args]))

(defn- conjv [coll v] (conj (vec coll) v))
(defn- str-from [n s] (apply str (repeat n s)))
(defn- indent [s spaces] (str (str-from spaces " ") s))
(defn- pad-right [s n]
  (let [padding (- n (count s))]
    (str s (str-from padding " "))))

(defn- -add-parameter [spec name description]
  (update spec :parameters conjv {:name name :description description :required? true}))

(defn- -add-optional-parameter [spec name description]
  (update spec :parameters conjv {:name name :description description}))

(defn- -add-multi-parameter [spec name description]
  (update spec :parameters conjv {:name name :description description :multi? true}))

(defn- -add-value-option
  ([spec short-name full-name value-description description]
   (-add-value-option spec short-name full-name value-description description false))
  ([spec short-name full-name value-description description multi?]
   (when-not (and short-name full-name)
     (throw (#?(:clj RuntimeException. :cljs js/Error. :cljr SystemException.) "Options require a shortName and fullName")))
   (let [option {:short-name        short-name
                 :full-name         full-name
                 :value-description value-description
                 :description       description
                 :multi?            multi?}]
     (update spec :options conjv option))))

(defn- -add-switch-option [spec short-name full-name description]
  (-add-value-option spec short-name full-name nil description))

(defn- -add-multi-option [spec short-name full-name value-description description]
  (-add-value-option spec short-name full-name value-description description true))

(defn- parameter->usage [{:keys [required? multi? name]}]
  (cond
    required? (str "<" name ">")
    multi? (str "[" name "*]")
    :else (str "[" name "]")))

(defn- -arg-string [{:keys [parameters options]}]
  (->> (cond-> (map parameter->usage parameters)
               (seq options)
               (conj "[options]"))
       (str/join " ")))

(defn- parameter->string [name-padding {:keys [name description]}]
  (str "  " (pad-right name name-padding) "  " description "\n"))

(defn- max-by-count [key-fn coll]
  (apply max 0 (map (comp count key-fn) coll)))

(defn- -parameters-string [spec]
  (let [name-padding (max-by-count :name (:parameters spec))]
    (str/join (map (partial parameter->string name-padding) (:parameters spec)))))

(def max-row-length 72)

(defn- split-by-line-length [description]
  ;; TODO [BAC]: Lookbehind for space characters
  (if (< max-row-length (count description))
    (map #(str/trim (apply str %)) (partition-all max-row-length description))
    [description]))

(defn- tabularize-description [padding description]
  (let [[first-line & rest-lines] (mapcat split-by-line-length (str/split-lines description))]
    (->> (map #(indent % padding) rest-lines)
         (cons first-line)
         (str/join "\n"))))

(defn- option->string [padding {:keys [header description]}]
  (let [padding (+ padding 2)]
    (str (pad-right header padding)
         (tabularize-description padding description)
         "\n")))

(defn- with-header [{:keys [short-name full-name value-description] :as option}]
  (let [value  (when value-description (str "=<" value-description ">"))
        header (str "  -" short-name ", --" full-name value)]
    (assoc option :header header)))

(defn- -options-string [{:keys [options]}]
  (let [options (map with-header options)
        padding (max-by-count :header options)]
    (str/join (map (partial option->string padding) options))))

(defn- split-arg [arg] (str/split arg #"="))

(defn- arg-name [arg]
  (if (str/starts-with? arg "--")
    (subs arg 2)
    (subs arg 1)))

(defn- option-named? [option name]
  (or (= name (:full-name option))
      (= name (:short-name option))))

(defn- find-option [options arg-name]
  (first (filter #(option-named? % arg-name) options)))

(defn- set-value [result key value multi?]
  (let [key (keyword key)]
    (if multi?
      (update result key conjv value)
      (assoc result key value))))

(defn- option? [arg]
  (str/starts-with? arg "-"))

(defn- nil-or-option? [v]
  (or (nil? v) (option? v)))

(defn- add-error [result error]
  (update result :*errors conjv error))

(defn- add-leftover [result arg]
  (update result :*leftover conjv arg))

(defn- last-multi-parameter? [parameters]
  (and (:multi? (first parameters))
       (empty? (rest parameters))))

(defn- formalize-option [options arg next-arg]
  (let [given-name (arg-name arg)
        option     (find-option options given-name)]
    {:raw-name   arg
     :given-name given-name
     :option     option
     :value      (if (:value-description option) next-arg "on")}))

(defn- formalize-parameter [parameter value]
  {:parameter parameter
   :value     value})

(defn- preprocess-option [{:keys [options] :as m} arg next-arg]
  (let [option (formalize-option options arg next-arg)]
    (-> (update m :args conj option)
        (assoc :skip? (:value-description (:option option))))))

(defn- preprocess-parameter [{:keys [parameters] :as m} arg]
  (let [parameter (formalize-parameter (first parameters) arg)]
    (cond-> (update m :args conj parameter)
            (not (last-multi-parameter? parameters))
            (update :parameters rest))))

(defn- preprocess-arg [m [arg next-arg]]
  (cond
    (:skip? m) (dissoc m :skip?)
    (option? arg) (preprocess-option m arg next-arg)
    :else (preprocess-parameter m arg)))

(defn- preprocess-args [spec args]
  (let [preprocessed       (->> (mapcat split-arg args)
                                (partition 2 1 nil)
                                (reduce preprocess-arg (assoc spec :args [])))
        missing-parameters (map #(formalize-parameter % nil) (filter :required? (:parameters preprocessed)))]
    (concat (:args preprocessed) missing-parameters)))

(defn- resolve-option [result {:keys [raw-name given-name option value]}]
  (if option
    (cond-> (set-value result (:full-name option) value (:multi? option))
            (nil-or-option? value)
            (add-error (str "Missing value for option: " given-name)))
    (-> result
        (add-error (str "Unrecognized option: " raw-name))
        (add-leftover raw-name))))

(defn- resolve-parameter [result {:keys [parameter value]}]
  (cond
    (not parameter)
    (-> result
        (add-error (str "Unexpected parameter: " value))
        (add-leftover value))
    (not value) (add-error result (str "Missing parameter: " (:name parameter)))
    :else (set-value result (:name parameter) value (:multi? parameter))))

(defn- resolve-arg [result argument]
  (if (contains? argument :option)
    (resolve-option result argument)
    (resolve-parameter result argument)))

(defn- -parse [spec args]
  (reduce resolve-arg {} (preprocess-args spec args)))

(defrecord Arguments []
  args/Arguments
  (add-multi-option [this short-name full-name value-description description]
    (-add-multi-option this short-name full-name value-description description))
  (add-multi-parameter [this name description]
    (-add-multi-parameter this name description))
  (add-optional-parameter [this name description]
    (-add-optional-parameter this name description))
  (add-parameter [this name description]
    (-add-parameter this name description))
  (add-switch-option [this short-name full-name description]
    (-add-switch-option this short-name full-name description))
  (add-value-option [this short-name full-name value-description description]
    (-add-value-option this short-name full-name value-description description))
  (parse [this args] (-parse this args))
  (arg-string [this] (-arg-string this))
  (parameters-string [this] (-parameters-string this))
  (options-string [this] (-options-string this)))

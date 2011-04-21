(ns speclj.components)

(defprotocol SpecComponent
  (install [this description]))

(extend-type java.lang.Object
  SpecComponent
  (install [this description]
    (throw (Exception. (str "Oops!  It looks like you tried to add a " (class this) ":" this " to a spec.  That's not allowed.")))))

(extend-type nil
  SpecComponent
  (install [this description]
    (throw (Exception. (str "Oops!  It looks like you tried to add 'nil' to a spec.  That's not allowed.")))))

(extend-type clojure.lang.Seqable
  SpecComponent
  (install [this description]
    (doseq [component (seq this)] (install component description))))

(deftype Description [name ns parent children charcteristics befores afters arounds before-alls after-alls withs]
  SpecComponent
  (install [this description]
    (reset! (.parent this) description)
    (swap! (.children description) conj this))
  Object
  (toString [this] (str "Description: " \" name \")))

(defn new-description [name ns]
  (Description. name ns (atom nil) (atom []) (atom []) (atom []) (atom []) (atom []) (atom []) (atom []) (atom [])))

(deftype Characteristic [name parent body pending]
  SpecComponent
  (install [this description]
    (reset! (.parent this) description)
    (swap! (.charcteristics description) conj this))
  Object
  (toString [this] (str \" name \")))

(defn pending-characteristic
  ([name] (Characteristic. name (atom nil) (fn [] true) true)))

(defn new-characteristic
  ([name body] (Characteristic. name (atom nil) body false))
  ([name description body] (Characteristic. name (atom description) body false)))

(deftype Before [body]
  SpecComponent
  (install [this description]
    (swap! (.befores description) conj this)))

(defn new-before [body]
  (Before. body))

(deftype After [body]
  SpecComponent
  (install [this description]
    (swap! (.afters description) conj this)))

(defn new-after [body]
  (After. body))

(deftype Around [body]
  SpecComponent
  (install [this description]
    (swap! (.arounds description) conj this)))

(defn new-around [body]
  (Around. body))

(deftype BeforeAll [body]
  SpecComponent
  (install [this description]
    (swap! (.before-alls description) conj this)))

(defn new-before-all [body]
  (BeforeAll. body))

(deftype AfterAll [body]
  SpecComponent
  (install [this description]
    (swap! (.after-alls description) conj this)))

(defn new-after-all [body]
  (AfterAll. body))

(deftype With [name body value]
  SpecComponent
  (install [this description]
    (swap! (.withs description) conj this))
  clojure.lang.IDeref
  (deref [this]
    (if (= ::none @value)
      (reset! value (body)))
    @value))

(defn reset-with [with]
  (reset! (.value with) ::none))

(defn new-with [name body]
  (With. name body (atom ::none)))

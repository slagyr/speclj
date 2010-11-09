(ns mmspec.components)

(deftype Description [name charcteristics befores afters before-alls after-alls])

(defn new-description [name]
  (Description. name (atom []) (atom []) (atom []) (atom []) (atom [])))

(defn- install-characteristic [characteristic description]
  (swap! (.description characteristic) (fn [_] description))
  (swap! (.charcteristics description) (fn [_] (conj _ characteristic))))

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
    (doseq [characteristic (seq this)] (install-characteristic characteristic description))))

(deftype Characteristic [name description body]
  SpecComponent
  (install [this description]
    (install-characteristic this description)))

(defn new-characteristic [name body]
  (Characteristic. name (atom nil) body))

(deftype Before [body]
  SpecComponent
  (install [this description]
    (swap! (.befores description) (fn [_] (conj _ this)))))

(defn new-before [body]
  (Before. body))

(deftype After [body]
  SpecComponent
  (install [this description]
    (swap! (.afters description) (fn [_] (conj _ this)))))

(defn new-after [body]
  (After. body))

(deftype BeforeAll [body]
  SpecComponent
  (install [this description]
    (swap! (.before-alls description) (fn [_] (conj _ this)))))

(defn new-before-all [body]
  (BeforeAll. body))

(deftype AfterAll [body]
  SpecComponent
  (install [this description]
    (swap! (.after-alls description) (fn [_] (conj _ this)))))

(defn new-after-all [body]
  (AfterAll. body))
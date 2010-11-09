(ns mmspec.components)

(deftype Description [name charcteristics befors])

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
(ns speclj.components)

(defprotocol SpecComponent
  (install [this description]))

#?(:clj
   (extend-protocol SpecComponent
     java.lang.Object
     (install [this description] (comment "This prohibits multimethod defs, and other stuff.  Don't be so stingy! Let it pass."))
     nil
     (install [this description] (throw (java.lang.Exception. (str "Oops!  It looks like you tried to add 'nil' to a spec.  That's probabaly not what you wanted."))))
     clojure.lang.Var
     (install [this description] (comment "Vars are cool.  Let them pass."))
     clojure.lang.Seqable
     (install [this description] (doseq [component (seq this)] (install component description))))

   :cljs
   (extend-protocol SpecComponent
     LazySeq
     (install [this description] (doseq [component (seq this)] (install component description)))
     List
     (install [this description] (doseq [component (seq this)] (install component description)))
     EmptyList
     (install [this description] (doseq [component (seq this)] (install component description)))
     PersistentVector
     (install [this description] (doseq [component (seq this)] (install component description)))
     nil
     (install [this description] (throw (ex-info (str "Oops!  It looks like you tried to add 'nil' to a spec.  That's probabaly not what you wanted.") {})))
     object
     (install [this description] (comment "Whatever...  Let them pass."))))

(deftype Description [name ns parent children charcteristics tags befores before-alls afters after-alls withs with-alls arounds around-alls]
  SpecComponent
  (install [this description]
    (reset! (.-parent this) description)
    (swap! (.-children description) conj this))
  Object
  (toString [this] (str "Description: " \" name \")))

(defn new-description [name ns]
  (Description. name ns (atom nil) (atom []) (atom []) (atom #{}) (atom []) (atom []) (atom []) (atom []) (atom []) (atom []) (atom []) (atom [])))

(deftype Characteristic [name parent body]
  SpecComponent
  (install [this description]
    (reset! (.-parent this) description)
    (swap! (.-charcteristics description) conj this))
  Object
  (toString [this] (str \" name \")))

(defn new-characteristic
  ([name body] (Characteristic. name (atom nil) body))
  ([name description body] (Characteristic. name (atom description) body)))

(deftype Before [body]
  SpecComponent
  (install [this description]
    (swap! (.-befores description) conj this)))

(defn new-before [body]
  (Before. body))

(deftype After [body]
  SpecComponent
  (install [this description]
    (swap! (.-afters description) conj this)))

(defn new-after [body]
  (After. body))

(deftype Around [body]
  SpecComponent
  (install [this description]
    (swap! (.-arounds description) conj this)))

(defn new-around [body]
  (Around. body))

(deftype BeforeAll [body]
  SpecComponent
  (install [this description]
    (swap! (.-before-alls description) conj this)))

(defn new-before-all [body]
  (BeforeAll. body))

(deftype AfterAll [body]
  SpecComponent
  (install [this description]
    (swap! (.-after-alls description) conj this)))

(defn new-after-all [body]
  (AfterAll. body))

(deftype AroundAll [body]
  SpecComponent
  (install [this description]
    (swap! (.-around-alls description) conj this)))

(defn new-around-all [body]
  (AroundAll. body))

#?(:clj
   (deftype With [name unique-name body value bang]
     SpecComponent
     (install [this description]
       (swap! (.-withs description) conj this))
     clojure.lang.IDeref
     (deref [this]
       (when (= ::none @value)
         (reset! value (body)))
       @value))

   :cljs
   (deftype With [name unique-name body value bang]
     SpecComponent
     (install [this description]
       (swap! (.-withs description) conj this))
     cljs.core/IDeref
     (-deref [this]
       (when (= ::none @value)
         (reset! value (body)))
       @value)))

(defn reset-with [with]
  (reset! (.-value with) ::none)
  (if (.-bang with) (deref with)))

(defn new-with [name unique-name body bang]
  (let [with (With. name unique-name body (atom ::none) bang)]
    (when bang (deref with)) ; TODO - MDM: This is the wrong place to deref.  Should do it in body right after arounds.
    with))

#?(:clj
   (deftype WithAll [name unique-name body value bang]
     SpecComponent
     (install [this description]
       (swap! (.-with-alls description) conj this))
     clojure.lang.IDeref
     (deref [this]
       (when (= ::none @value)
         (reset! value (body)))
       @value))

   :cljs
   (deftype WithAll [name unique-name body value bang]
     SpecComponent
     (install [this description]
       (swap! (.-with-alls description) conj this))
     cljs.core/IDeref
     (-deref [this]
       (when (= ::none @value)
         (reset! value (body)))
       @value)))

(defn new-with-all [name unique-name body bang]
  (let [with-all (WithAll. name unique-name body (atom ::none) bang)]
    (when bang (deref with-all))
    with-all))

(deftype Tag [name]
  SpecComponent
  (install [this description]
    (swap! (.-tags description) conj name)))

(defn new-tag [name]
  (Tag. name))

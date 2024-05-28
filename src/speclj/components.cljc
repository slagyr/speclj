(ns speclj.components)

(defprotocol SpecComponent
  (install [this description]))

#?(:clj
   (extend-protocol SpecComponent
     java.lang.Object
     (install [_this _description] (comment "This prohibits multimethod defs, and other stuff.  Don't be so stingy! Let it pass."))
     nil
     (install [_this _description] (throw (java.lang.Exception. (str "Oops!  It looks like you tried to add 'nil' to a spec.  That's probably not what you wanted."))))
     clojure.lang.Var
     (install [_this _description] (comment "Vars are cool.  Let them pass."))
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
     (install [_this _description] (throw (ex-info (str "Oops!  It looks like you tried to add 'nil' to a spec.  That's probably not what you wanted.") {})))
     object
     (install [_this _description] (comment "Whatever...  Let them pass."))))

(deftype Description [name is-focused? has-focus? ns parent children characteristics tags befores before-alls afters after-alls withs with-alls arounds around-alls]
  SpecComponent
  (install [this description]
    (reset! (.-parent this) description)
    (swap! (.-children description) conj this))
  Object
  (toString [_this] (str "Description: " \" name \")))

(defn new-description [name is-focused? ns]
  (Description. name (atom is-focused?) (atom false) ns (atom nil) (atom []) (atom []) (atom #{}) (atom []) (atom []) (atom []) (atom []) (atom []) (atom []) (atom []) (atom [])))

(defn is-description? [component]
  (instance? Description component))

(deftype Characteristic [name parent body is-focused?]
  SpecComponent
  (install [this description]
    (reset! (.-parent this) description)
    (swap! (.-characteristics description) conj this))
  Object
  (toString [_this] (str \" name \")))

(defn new-characteristic
  ([name body is-focused?] (Characteristic. name (atom nil) body (atom is-focused?)))
  ([name description body is-focused?] (Characteristic. name (atom description) body (atom is-focused?))))

(defn is-characteristic? [component]
  (instance? Characteristic component))

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

(deftype With [name body set-var! value bang]
  SpecComponent
  (install [this description]
    (swap! (.-withs description) conj this))
  #?(:clj clojure.lang.IDeref :cljs cljs.core/IDeref)
  (#?(:clj deref :cljs -deref) [_this]
    (when (= ::none @value)
      (reset! value (body)))
    @value))

(defn reset-with [with]
  (reset! (.-value with) ::none)
  (when (.-bang with) (deref with)))

(defn new-with [name body set-var! bang]
  (let [with (With. name body set-var! (atom ::none) bang)]
    (when bang (deref with)) ; TODO - MDM: This is the wrong place to deref.  Should do it in body right after arounds.
    with))

(deftype WithAll [name body set-var! value bang]
  SpecComponent
  (install [this description]
    (swap! (.-with-alls description) conj this))
  #?(:clj clojure.lang.IDeref :cljs cljs.core/IDeref)
  (#?(:clj deref :cljs -deref) [_this]
    (when (= ::none @value)
      (reset! value (body)))
    @value))

(defn new-with-all [name body set-var! bang]
  (let [with-all (WithAll. name body set-var! (atom ::none) bang)]
    (when bang (deref with-all))
    with-all))

(deftype Tag [name]
  SpecComponent
  (install [_this description]
    (swap! (.-tags description) conj name)))

(defn new-tag [name]
  (Tag. name))

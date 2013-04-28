(ns specljs.platform)

(defmacro new-throwable [message] message)
(defmacro new-exception [message] `(js/Error. ~message))
(defmacro new-failure [message] `(specljs.platform.SpecFailure. ~message))
(defmacro new-pending [message] `(specljs.platform.SpecPending. ~message))
(defmacro throw-error [message] `(throw (js/Error. ~message)))
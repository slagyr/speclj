(ns speclj.args)

(defprotocol Arguments
  ;; (add-parameter [_this _name _description])
  ;; (add-optional-parameter [_this _name _description])
  (add-multi-parameter [_this _name _description])
  (add-switch-option [_this _short-name _full-name _description])
  (add-value-option [_this _short-name _full-name _value-description _description])
  (add-multi-option [_this _short-name _full-name _value-description _description])
  (parse [_this _args])
  (arg-string [_this])
  (parameters-string [_this])
  (options-string [_this]))

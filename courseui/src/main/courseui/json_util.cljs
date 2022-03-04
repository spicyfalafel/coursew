(ns courseui.json-util)

(defn parse-string [str]
  (js->clj (.parse js/JSON str) :keywordize-keys true))

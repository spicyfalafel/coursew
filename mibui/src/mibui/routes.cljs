(ns mibui.routes
  (:require
   [bidi.bidi :as bidi]
   [pushy.core :as pushy]
   [re-frame.core :as re-frame]
   [mibui.events :as events]))

(defmulti panels identity)
(defmethod panels :default [] [:div "No such page"])

(def routes
  (atom
    ["/" {""      :home
          "login" :login
          "register" :register
          "my-aliens" :my-aliens}]))

(defn parse
  [url]
  (bidi/match-route @routes url))


(defn url-for
  [& args]
  (apply bidi/path-for (into [@routes] args)))

(defn dispatch
  [route]
  (let [panel (keyword (str (name (:handler route)) "-panel"))]
    (re-frame/dispatch [::events/set-active-panel panel])))

(defonce history
  (pushy/pushy dispatch parse))

(defn navigate!
  [handler]
  (pushy/set-token! history (url-for handler)))

(defn start!
  []
  (pushy/start! history))

(re-frame/reg-fx
  :navigate
  (fn [handler]
    (navigate! handler)))


;; -- url-for -----------------------------------------------------------------
;; To dispatch routes in our UI (view) we will use url-for and then pass a
;; keyword to which route we want to direct the user.
;; usage: (url-for :home)
(def get-url-for (partial bidi/path-for routes))

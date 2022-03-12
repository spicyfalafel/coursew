(ns mibui.routes
  (:require
   [bidi.bidi :as bidi]
   [pushy.core :as pushy]
   [re-frame.core :as re-frame :refer [dispatch]]))

(def routes
  ["/" {""         :home
        "login"    :login
        "register" :register
        "my-aliens/" { "" :my-aliens
                      [:id ""] :alien-view}
        "another"  :another}])


;; -- History -----------------------------------------------------------------
;; we need to know the history of our routes so that we can navigate back and
;; forward. For that we'll use `pushy/pushy`, to which we need to provide a dispatch
;; function (what happens on dispatch) and match (what routes should we match).
(def history
  (let [dispatch #(dispatch [:set-active-page {:page (:handler %)}])
        match #(bidi/match-route routes %)]
    (println "history")
    (pushy/pushy dispatch match)))

;; -- Router Start ------------------------------------------------------------
;;
(defn start!
  []
  ;; pushy is here to take care of nice looking urls. Normally we would have to
  ;; deal with #. By using pushy we can have '/about' instead of '/#/about'.
  ;; pushy takes three arguments:
  ;; dispatch-fn - which dispatches when a match is found
  ;; match-fn - which checks if a route exist
  ;; identity-fn (optional) - extract the route from value returned by match-fn
  (pushy/start! history))

;; -- url-for -----------------------------------------------------------------
(defn url-for [& [main id-key id-val]]
  (bidi/path-for routes main id-key id-val))

;; -- set-token! --------------------------------------------------------------
;; To change route after some actions we will need to set url and for that we
;; will use set-token!, taking the history and a token.
(defn set-token!
  [token]
  (println "set token")
  (pushy/set-token! history token))

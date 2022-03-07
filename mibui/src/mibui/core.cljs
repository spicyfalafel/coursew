(ns mibui.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [mibui.events :as events]
   [mibui.routes :as routes]
   [mibui.views :as views]
   [mibui.config :as config]))



(defn dev-setup [])

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (routes/start!)
  (println "initialize-db")
  (re-frame/dispatch-sync [:initialize-db])
  (println "dev setup!")
  (dev-setup)
  (println "mount-root")
  (mount-root))

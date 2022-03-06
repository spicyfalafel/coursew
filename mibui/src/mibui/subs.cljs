(ns mibui.subs
  (:require
   [re-frame.core :as re-frame :refer [reg-sub]]))


(reg-sub
 :active-page
 (fn [db _]
   (:active-page db)))


(reg-sub
 :user ;; usage: (subscribe [:user])
 (fn [db _]
   (:user db)))

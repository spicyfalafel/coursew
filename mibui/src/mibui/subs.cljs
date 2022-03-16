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

(reg-sub
 :errors ;; usage: (subscribe [:errors])
 (fn [db _]
   (:errors db)))

(reg-sub
 :my-aliens
 (fn [db _]
   (:my-aliens db)))

(reg-sub
 :alien-view
 (fn [db _]
   (:alien-view db)))

(reg-sub
 :alien-form
 (fn [db _]
   (:alien-form db)))

(reg-sub
 :requests
 (fn [db _]
     (:requests db)))

(reg-sub
 :request
 (fn [db _]
   (:request db)))

(reg-sub
 :professions
 (fn [db _]
   (:professions db)))

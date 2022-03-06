(ns mibui.views
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as reagent]
   [mibui.events :as events]
   [mibui.routes :as routes :refer [url-for]]
   [mibui.subs :as subs]))

(defn home []
  [:div "this is home panel"])

;; login
(defn login []
  (let [default {:login "" :password ""}
        credentials (reagent/atom default)
        login-user (fn [event credentials]
                     (.preventDefault event)
                     (dispatch [:login credentials]))]
    (fn []
      (let [{:keys [username password]} @credentials]
       [:div.row.align-items-center.justify-content-center
        [:form.col-4.text-center.w-25 {:on-submit #(login-user % @credentials)}
         [:h1.text-xs-center "Sign in"]
         [:div.form-outline
          [:input.form-control {:id :login
                                :type :text
                                :value username
                                :on-change #(swap! credentials assoc :username
                                                   (-> % .-target .-value))}]
          [:label.form-label {:for :login} "Username"]]
         [:div.form-outline
          [:input.form-control {:id :passw
                                :type :password
                                :value password
                                :on-change #(swap! credentials assoc :password
                                                   (-> % .-target .-value))}]
          [:label.form-label {:for :passw} "Password"]]
         [:button.btn.btn-primary.btn-block {:type :submit} "OK"]]]))))

(defn register []
  [:div "register panel"])

(defn header []
  (let [user @(subscribe [:user])
        active-page @(subscribe [:active-page])]
      [:div
       [:nav.navbar.navbar-light.navbar-expand-lg.bg-light
         [:a.navbar-brand {:href (url-for :home)} "Men In Black"]
         (if (empty? user)
           [:div.collapse.navbar-collapse
            [:ul.navbar-nav.mr-auto
             [:li.nav-item.active
              [:a.nav-link {:href (url-for :home) } "Home"]]
             [:li.nav-item
              [:a.nav-link {:href (url-for :login)}  "Sign in"]]
             [:li.nav-item
              [:a.nav-link {:href (url-for :register)}  "Sign up"]]]]
           ;; if not empty user
           [:div.collapse.navbar-collapse
            [:ul.navbar-nav.mr-auto
             [:li.nav-item.active
              [:a.nav-link {:href (url-for :home) } "Home"]]
             [:li.nav-item.active
              [:a.nav-link {:href (url-for :aliens)}  "My aliens"]]
             [:li.nav-item.active
              [:a.nav-link  {:on-click
                             #(.preventDefault %
                               (dispatch [:logout]))} "Log out"]]]])]]))


(defn aliens []
  [:div "my aliens"])


(defn another []
  [:div "again"])

(defn pages
  [page-name]
  (case page-name
    :home [home]
    :login [login]
    :register [register]
    :aliens [aliens]
    :another [another]
    [home]))

;; main

(defn main-panel []
  (let [active-page @(subscribe [:active-page])]
    (println "main panel")
    [:div
     [header]
     [pages active-page]]))

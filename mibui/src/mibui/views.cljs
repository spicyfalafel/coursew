(ns mibui.views
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as reagent]
   [mibui.events :as events]
   [mibui.routes :as routes :refer [get-url-for url-for]]
   [mibui.subs :as subs]))

(defn home-panel []
  [:div "this is home panel"])


(defmethod routes/panels :home-panel [] [home-panel])

;; login

(defn login-panel []
  (let [default {:login "" :password ""}
        credentials (reagent/atom default)
        login-user (fn [event credentials]
                     (.preventDefault event)
                     (dispatch [:login credentials]))]
    (fn []
      (let [{:keys [login password]} @credentials]
       [:div.row.align-items-center.justify-content-center
        [:form.col-4.text-center.w-25 {:on-submit #(login-user % @credentials)}
         [:h1.text-xs-center "Sign in"]
         [:div.form-outline
          [:input.form-control {:id :login
                                :type :text
                                :value login
                                :on-change #(swap! credentials assoc :login
                                                   (-> % .-target .-value))}]
          [:label.form-label {:for :login} "Login"]]
         [:div.form-outline
          [:input.form-control {:id :passw
                                :type :password
                                :value password
                                :on-change #(swap! credentials assoc :password
                                                   (-> % .-target .-value))}]
          [:label.form-label {:for :passw} "Password"]]
         [:h1 (:login @credentials)]
         [:h1 (:password @credentials)]
         [:button.btn.btn-primary.btn-block {:type :submit} "OK"]]]))))

(defmethod routes/panels :login-panel [] [login-panel])

(defn register-panel []
  [:div "register panel"])

(defmethod routes/panels :register-panel [] [register-panel])


(defn my-aliens-panel []
  [:div

   [:h1 "This is my aliens page"]])

(defmethod routes/panels :my-aliens-panel [] [my-aliens-panel])

; (defn header [])
    ; [:> ThemeProvider {:theme dark-theme}
    ;  [:> Box {:sx {:flexGrow 1}}
    ;   [:> AppBar {:position :static
    ;               :color "primary"}
    ;    [:> Toolbar
    ;     [:> Typography "Men In Black"]
    ;     [:> Button {:color :inherit} "BUTTON"]]]]])
    ;



(defn header []
               ; (let [ ;user @(subscribe [:user])
               ;       active-panel @(subscribe [::subs/active-panel])]
      [:div
       [:nav.navbar.navbar-light.navbar-expand-lg.bg-light
         [:a.navbar-brand {:href (url-for :home)} "Men In Black"]
         [:div.collapse.navbar-collapse
          [:ul.navbar-nav.mr-auto
           [:li.nav-item.active
            [:a.nav-link {:href (url-for :home) } "Home"]]
           [:li.nav-item
            [:a.nav-link {:href (url-for :login)}  "Sign in"]]
           [:li.nav-item
            [:a.nav-link {:href (url-for :register)}  "Sign up"]]]]]]); [:li.nav-item
            ;  [:a.nav-link {:href (url-for :register "Sign up")}]]]]]]])
           ;  ;; if user not empty
       ;  [:li.nav-item
       ;   [:a.nav-link {:href (get-url-for :home) :class (when (= active-page :home) "active")} "Home"]]
       ;  [:li.nav-item
       ;   [:a.nav-link {:href (get-url-for :editor :slug "new") :class (when (= active-page :editor) "active")}
       ;    [:i.ion-compose "New Article"]]]
       ;  [:li.nav-item
       ;   [:a.nav-link {:href (get-url-for :settings) :class (when (= active-page :settings) "active")}
       ;    [:i.ion-gear-a "Settings"]]]
       ;  [:li.nav-item
       ;   [:a.nav-link {:href (get-url-for :profile :user-id (:username user)) :class (when (= active-page :profile) "active")} (:username user)
       ;    [:img.user-pic {:src (:image user) :alt "user image"}]]]]]]]))
       ;


;; main

(defn main-panel []
  (let [active-panel (subscribe [::subs/active-panel])]
    [:div
     [header]
     (routes/panels @active-panel)]))

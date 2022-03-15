(ns mibui.views
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as reagent]
   [mibui.events :as events]
   [mibui.routes :as routes :refer [url-for]]
   [mibui.subs :as subs]
   [cljs-time.core :as time]
   [cljs-time.format :as time-format]))


(defn radio [label name value]
  [:div.radio
   [:label
    [:input {:field :radio :name name :value value}]
    label]])

(defn home []
  [:div "this is home panel"])

(defn errors-list
  [errors]
  [:ul.error-messages
   (for [[k [v]] errors]
     ^{:key k} [:li (str (name k) " " v)])])


; login
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


(defn register
  []
  (let [default {:username "" :password "" :alien false}
        registration (reagent/atom default)]
    (fn []
      (let [{:keys [username password alien]} @registration
            errors @(subscribe [:errors])
            register-user (fn [event registration]
                            (.preventDefault event)
                            (dispatch [:register-user registration]))]

         [:div.row.align-items-center.justify-content-center
            [:form.col-4.text-center.w-25 {:on-submit #(register-user % @registration)}
             [:h1 "Sign up"]

             [:div.form-outline
              [:input.form-control {:id :username
                                    :type        "text"
                                    :value       username
                                    :on-change   #(swap! registration assoc :username (-> % .-target .-value))}]
              [:label.form-label {:for :username} "Username"]]

             [:div.form-outline
                 [:input.form-control {:id :pass
                                       :type        "password"
                                       :value       password
                                       :on-change   #(swap! registration assoc :password (-> % .-target .-value))}]
                 [:label.form-label {:for :pass} "Password"]]

             [:div.form-check.d-flex.justify-content-center
              [:input.form-check-input.m-1 {:id :alien
                                            :type "checkbox"
                                            :value alien
                                            :on-change
                                            (fn [_] (doall (println (not alien))
                                                           (swap! registration assoc :alien (not alien))))}]
                                                        ;                                      not
                                                        ;                                      some?)))}]

              [:label {:class "form-check-label", :for :alien} "I am alien"]]
             [:div.form-outline.mt-3
              [:button.btn.btn-primary.pull-xs-right "OK"]]

             [:p.text-xs-center
              [:a {:href (url-for :login)} "Have an account?"]]]]))))
; (when (:register-user errors) [errors-list (:register-user errors)])



(defn aliens []
  (let [my-aliens @(subscribe [:my-aliens])]
    [:div.align-items-center.justify-content-center.row
     [:h1 "My aliens"]
     (for [alien  my-aliens]
      [:div.col.m-3 {:key (:alien_info_id alien)}
       [:div.card {:style {:width "18rem"}}
        (if (:user_photo alien)
          (:user_photo alien)
          [:img.card-img-top {:src "/user.jpg"}])
        [:div.card-body
         [:h5.card-title (:username alien)]
         [:h6.card-subtitle.mb-2.text-muted "id " (:alien_info_id alien)]
         ; [:h6.card-subtitle.mb-2.text-muted  (:status alien)]
         [:h6.card-subtitle.mb-2.text-muted  "Departure date " (:departure_date alien)]

         [:button.btn.card-link.btn-primary {
                                             :on-click #(.preventDefault %
                                                                         (dispatch [:alien-view (:alien_info_id alien)]))}
          (if (:reported alien) "Rated" "Rate behavior")]]]])]))


(defn alien []
 (let [al @(subscribe [:alien-view])
       default {:behavior 1 :description ""
                :agent_info_id (:agent_info_id @(subscribe [:user]))
                :report_date (time-format/unparse (time-format/formatter "yyyy-MM-dd") (time/today))}
       report (reagent/atom default)
       change-rating #(swap! report assoc :behavior (-> % .-target .-value))
       send-report (fn [event report]
                     (.preventDefault event)
                     (dispatch [:send-report (:alien_info_id al) report]))]
    (fn []
      (let [{:keys [behavior description report_date]} @report]
       [:div.container
        [:div
         [:h2 "Alien " (:username al) "(" (:alien_info_id al) ")"]
         [:img.rounded.float-start {:src "/user.jpg"}]
         [:h5 "Personality"]
         [:p "First name: " (:first_name al)
          [:br] "Second name: " (:second_name al)
          [:br] "Age: " (:age al)
          [:br] "Status: " (:status al)
          [:br] "Country: " (:country al)
          [:br] "City: " (:city al)
          [:br] "Profession: " (:profession_name al)]]
        [:form.align-items-center.text-center {:on-submit #(send-report % @report)}
         [:br] [:br] [:br]
         [:h2.text-center "Daily Report " report_date]
         ; [:form {:on-submit #(send-report % @report)}]
         [:div.row
          [:div.mb-3.col.text-center
           [:label.form-label {:for "descriptionTextarea"} "Description"]
           [:textarea.form-control { :id "descriptionTextarea", :rows "3"
                                    :on-change #(swap! report assoc :description (-> % .-target .-value))}]]
          [:div.align-items-center.col.text-center.mb-3
           [:label.form-label {:for "rating"} "Rate behavior"]
           [:div.card.align-items-center {:id "rating"}
            [:div.card-body.text-center
             [:fieldset.rating
              [:input {:type "radio", :id "star5", :name "rating", :value "10"
                       :on-change #(change-rating %)}]
              [:label.full {:for "star5", :title "Awesome - 10/10"}]
              [:input {:type "radio", :id "star4half", :name "rating", :value "9"
                       :on-change #(change-rating %)}]
              [:label.half { :for "star4half", :title "Pretty good - 9/10"}]
              [:input {:type "radio", :id "star4", :name "rating", :value "8"
                       :on-change #(change-rating %)}]
              [:label.full { :for "star4", :title "Pretty good - 8/10"}]
              [:input {:type "radio", :id "star3half", :name "rating", :value "7"
                       :on-change #(change-rating %)}]
              [:label.half { :for "star3half", :title "Meh - 7/10"}]
              [:input {:type "radio", :id "star3", :name "rating", :value "6"
                       :on-change #(change-rating %)}]
              [:label.full {:for "star3", :title "Meh - 6/10"}]
              [:input {:type "radio", :id "star2half", :name "rating", :value "5"
                       :on-change #(change-rating %)}]
              [:label.half {:for "star2half", :title "Kinda bad - 5/10"}]
              [:input {:type "radio", :id "star2", :name "rating", :value "4"
                       :on-change #(change-rating %)}]
              [:label.full {:for "star2", :title "Kinda bad - 4/10"}]
              [:input {:type "radio", :id "star1half", :name "rating", :value "3"
                       :on-change #(change-rating %)}]
              [:label.half {:for "star1half", :title "Meh - 3/10"}]
              [:input {:type "radio", :id "star1", :name "rating", :value "2"
                       :on-change #(change-rating %)}]
              [:label.full {:for "star1", :title "Sucks big time - 2/10"}]
              [:input {:type "radio", :id "starhalf", :name "rating", :value "1"
                       :on-change #(change-rating %)}]
              [:label.half {:for "starhalf", :title "Sucks big time - 1/10"}]
              [:input.reset-option {:type "radio", :name "rating", :value "reset"}]" "]]]]]
         [:button.btn.btn-primary.w-25 "Save"]
         [:div behavior " " description]]]))))

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
            [:ul.navbar-nav.me-auto
             [:li.nav-item.active
              [:a.nav-link {:href (url-for :home) } "Home"]]
             [:li.nav-item.active
              [:a.nav-link {:href (url-for :my-aliens)}  "My aliens"]]]

            [:ul.navbar-nav
             [:li.nav-item.navbar-text (str (if (:agent_info_id user)
                                              "AGENT "
                                              "ALIEN ")
                                         (:username user))]
             [:li.nav-item.active
              [:a.nav-link  {:on-click
                             #(.preventDefault %
                               (dispatch [:logout]))} "Log out"]]]])]]))

(defn pages
  [page-name]
  (case page-name
    :home [home]
    :login [login]
    :register [register]
    :my-aliens [aliens]
    :alien-view [alien]
    [home]))



;; main

(defn main-panel []
  (let [active-page @(subscribe [:active-page])]
    [:div
     [header]
     [pages active-page]]))

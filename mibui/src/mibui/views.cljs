(ns mibui.views
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as reagent]
   [mibui.routes :as routes :refer [url-for]]
   [cljs-time.core :as time]
   [cljs-time.format :as time-format]
   [clojure.string :as str]))


(defn radio [label name value]
  [:div.radio
   [:label
    [:input {:field :radio :name name :value value}]
    label]])

(defn home []
  (let [user @(subscribe [:user])
        is-agent (:agent_info_id user)]
    (dispatch [:set-active-page {:page
                                 (if (empty? user)
                                   :login
                                   (if is-agent
                                     :my-aliens
                                     :alien-form))}])))


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
                                            (fn [_] (swap! registration assoc :alien (not alien)))}]

              [:label {:class "form-check-label", :for :alien} "I am alien"]]
             [:div.form-outline.mt-3
              [:button.btn.btn-primary.pull-xs-right "OK"]]

             [:p.text-xs-center
              [:a {:href (url-for :login)} "Have an account?"]]]]))))



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
      (let [{:keys [report_date]} @report]
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
         [:button.btn.btn-primary.w-25 "Save"]]]))))


(defn header []
  (let [user @(subscribe [:user])
        is-agent (:agent_info_id user)]
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
             (when is-agent [:li.nav-item.active
                             [:a.nav-link {:href (url-for :my-aliens)} "My aliens"]])
             (when is-agent [:li.nav-item.active
                             [:a.nav-link {:href (url-for :requests)}  "Requests"]])

             (when (not is-agent) [:li.nav-item.active
                                   [:a.nav-link {:href (url-for :alien-form)} "Alien form"]])]
            [:ul.navbar-nav
             [:li.nav-item.navbar-text (str (if is-agent
                                              "AGENT "
                                              "ALIEN ")
                                         (:username user))]
             [:li.nav-item.active
              [:a.nav-link  {:on-click
                             #(.preventDefault %
                               (dispatch [:logout]))} "Log out"]]]])]]))


(defn requests-view []
  (let [requests @(subscribe [:requests])]
    [:div.align-items-center.justify-content-center.row
       (map (fn [req]
              [:div.card.m-3 {:key (:request_id req)
                              :style {:max-width "540px"}}
               [:div.row.g-0
                [:div.col-md-4
                 [:img.img-fluid.rounded-start.mt-5 {:src "/user.jpg"}]]
                [:div.col-md-8
                 [:div.card-body
                   [:h5.card-title "Request #" (:request_id req)]
                   [:h6.card-subtitle.mb-2.text-muted "Username: " (:username req)]
                   [:h6.card-subtitle.mb-2.text-muted "Type: " (:type req)]
                   [:h6.card-subtitle.mb-2.text-muted  "Status: " (:status req)]
                   [:a.btn.card-link.btn-primary

                     {:on-click #(dispatch [:request (:request_id req)])}
                    "Check out"]]]]])
         requests)]))

(defn alien-form []
  (let [user @(subscribe [:user])
        default {:userid (:user_id user)
                 :planet_name ""
                 :visit_purp ""
                 :staytime 1
                 :comm ""
                 :skills []}
        form-from-db @(subscribe [:alien-form])
        al-form (reagent/atom (if (empty? form-from-db)
                                default
                                form-from-db))
        send-form (fn [event form]
                    (.preventDefault event)
                    (dispatch [:send-alien-form form]))]
    (fn []
      (let [{:keys [planet_name visit_purp staytime comm skills]} @al-form]
        [:div.row.align-items-center.justify-content-center
           [:form.col-4.text-center.w-25 {:on-submit #(send-form % @al-form)}
            (println form-from-db)
            [:h1 "Alien form"]
            [:div.form-outline
             [:input.form-control {:id :planet
                                   :type        "text"
                                   :value       planet_name
                                   :on-change   #(swap! al-form assoc :planet_name (-> % .-target .-value))}]
             [:label.form-label {:for :planet} "Planet"]]
            [:div.form-outline
                [:input.form-control {:id :visit-purpose
                                      :type        "text"
                                      :value       visit_purp
                                      :on-change   #(swap! al-form assoc :visit_purp (-> % .-target .-value))}]
                [:label.form-label {:for :visit-purpose} "Visit purpose"]]
            [:div.form-outline
                [:input.form-control {:id :visit-purpose
                                      :type        "text"
                                      :value       staytime
                                      :on-change   #(swap! al-form assoc :staytime (-> % .-target .-value int))}]
                [:label.form-label {:for :visit-purpose} "Stay time in days"]]
            [:div.form-outline
                [:textarea.form-control {:id :skills
                                         :type        "text"
                                         :value skills
                                         :on-change   #(swap! al-form assoc
                                                              :skills (-> % .-target .-value))}]

                [:div.form-text "E.g. \"skill1,skill2,skill3...\""]
                [:label.form-label {:for :visit-purpose} "Skills"]]
            [:div.form-outline
                [:input.form-control {:id :comment
                                      :type        "text"
                                      :value       comm
                                      :on-change   #(swap! al-form assoc :comm (-> % .-target .-value))}]
                [:label.form-label {:for :comment} "Comment"]]
            (when (empty? form-from-db)
              [:div.form-outline.mt-3
               [:button.btn.btn-primary.pull-xs-right "Send request"]])]]))))


(defn request-view []
  (let [user @(subscribe [:user])
        req @(subscribe [:request])
        professions @(subscribe [:professions])
        {:keys [request_id username date request_type status creator_id
                planet_name race visit_purpose stay_time comment skills]}
        req
        _ (dispatch [:skills-by-user-id creator_id])
        status-here (reagent/atom :pending)
        reject (fn [event req-id]
                 (.preventDefault event)
                 (dispatch [:reject-request req-id])
                 (reset! status-here :rejected))
        form-inputs (reagent/atom {:request_id (:request_id req)
                                   :creatorid (:creator_id req)
                                   :executorid (:user_id user)})
        accept (fn [event data]
                 (.preventDefault event)
                 (dispatch [:accept-request request_id data])
                 (reset! status-here :accepted))]
    (fn []
      (case @status-here
        :pending [:div.container
                  [:div
                   [:br]
                   [:h3.text-center "Request"]
                   [:img.rounded.float-start {:src "/user.jpg"}]
                   [:br] "Username: " username ;creator_id
                   [:br] "Request id: " request_id
                   [:br] "Created date: " date
                   [:br] "Request type: " request_type
                   [:br] "Status: " status
                   [:br] "Planet: " planet_name
                   [:br] "Race: " race
                   [:br] "Visit purpose: " visit_purpose
                   [:br] "Stay time: " stay_time " days"
                   [:br] "Comment: " comment]
                  [:div "Skills: " (for [skill skills] (str skill " "))]
                  [:div.row.align-items-center.justify-content-center
                   [:button.btn.btn-primary.w-25  {:on-click #(reject % request_id)} "Reject"]

                   [:button.btn.btn-primary.m-3.w-25 {:on-click
                                                      #(accept % @form-inputs)} "Accept"]]
                  [:div.row.align-items-center.justify-content-center
                   [:h3.text-center "Personality"]
                   [:form.col-4.text-center ;{:on-submit #(send-form % @al-form)}
                    [:div.form-outline.mb-2
                     [:input.form-control {:id :first_name
                                           :type        "text"
                                           ; :value       planet_name
                                           :on-change   #(swap! form-inputs assoc :firstname (-> % .-target .-value))}]
                     [:label.form-label {:for :first_name} "First name"]]
                    [:div.form-outline.mb-2
                        [:input.form-control {:id :second_name
                                              :type        "text"
                                              ; :value       visit_purp
                                              :on-change   #(swap! form-inputs assoc :secondname (-> % .-target .-value))}]
                        [:label.form-label {:for :second_name} "Second name"]]
                    [:div.form-outline.mb-2
                        [:input.form-control {:id :age
                                              :type        "text"
                                              ; :value       staytime
                                              :on-change   #(swap! form-inputs assoc :agearg (-> % .-target .-value int))}]
                        [:label.form-label {:for :age} "Age"]]
                    [:div.form-outline.mb-2
                        [:input.form-control {:id :profession
                                              :type        "text"
                                              ; :value skills
                                              :on-change   #(swap! form-inputs assoc
                                                                   :professionname (-> % .-target .-value))}]
                        [:label.form-label {:for :profession} "Profession"]
                     (doall [:div
                             [:div.form-text "Available professions for this alien: " (str/join " " (map :name professions))]])]


                    [:div.form-outline.mb-2
                        [:input.form-control {:id :country
                                              :type        "text"
                                              ; :value       comm
                                              :on-change   #(swap! form-inputs assoc :countryname (-> % .-target .-value))}]
                        [:label.form-label {:for :country} "Country"]]
                    [:div.form-outline.mb-2
                        [:input.form-control {:id :city
                                              :type        "text"
                                              ; :value       comm
                                              :on-change   #(swap! form-inputs assoc :cityname (-> % .-target .-value))}]
                        [:label.form-label {:for :city} "City"]]]]]

        :rejected [:div "Rejected request " request_id]
        :accepted [:div "Accepted request " request_id]))))

(defn pages
  [page-name]
  (case page-name
    ;; all
    :home [home]
    :login [login]
    :register [register]
    ;; agent
    :my-aliens [aliens]
    :alien-view [alien]
    :requests [requests-view]
    :request [request-view]
    ;; alien
    :alien-form [alien-form]
    [home]))



;; main

(defn main-panel []
  (let [active-page @(subscribe [:active-page])]
    [:div
     [header]
     [pages active-page]]))

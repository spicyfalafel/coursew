
(ns samurai-ui.app
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [samurai-ui.requests :as myreq]
            ["@material-ui/core/Button" :default Button]
            ["@material-ui/core/TextField" :default TextField]
            ["@material-ui/core/Container" :default Container]
            ["@material-ui/core/Box" :default Box]
            ["@material-ui/core/Card" :default Card]
            ["@material-ui/core/CardActions" :default CardActions]
            ["@material-ui/core/CardContent" :default CardContent]))





(defn init []
  (println "app initialized")
  (myreq/get-patients))


(defn Application []
  [:> Container {:maxWidth :xl}
   [:> Box {:sx {:bgcolor "#cfe8fc"}}
    [:h1.app-name "Patients"]
    [:form.add-patient-form

     [:h2 "Add patient"]
     [:> Button {:onClick (fn [] (js/alert "not now"))
                 :color :primary :variant :contained} "Submit"]
     [:div
      [:> TextField {:variant :outlined
                     :label "Firstname"}]]
     [:div
       [:> TextField {:variant :outlined
                      :label "Secondname"}]]
     [:div
       [:> TextField {:variant :outlined
                      :label "Gender"}]]
     [:div
      [:> TextField {:variant :outlined
                     :label "Address"}]]
     [:div
       [:> TextField {:variant :outlined
                      :label "Polys number"}]]]

    (doall
     (for [p @myreq/patients-atom]
       [:> Box {:sx {:maxWidth 200}}
        [:> Card
         [:> CardContent
          [:> TextField {:label "Firstname"
                         :size :small :variant :filled
                         :fullWidth true
                         :defaultValue (:firstname p)}]
          [:> TextField {:label "Lastname" :defaultValue (:lastname p)}]
          [:> TextField {:label "Birthdate" :defaultValue (:birthdate p)}]
          [:> TextField {:label "Gender" :defaultValue (:gender p)}]
          [:> TextField {:label "Addressname" :defaultValue (:address p)}]
          [:> TextField {:label "Polys" :defaultValue (:polys p)}]]]]))]])


(dom/render [Application] (js/document.getElementById "app"))

(ns mibui.views
  (:require
   [re-frame.core :as re-frame]
   [mibui.events :as events]
   [mibui.routes :as routes]
   [mibui.subs :as subs]
   ["@mui/material/Button" :default Button]
   ["@mui/material/TextField" :default TextField]
   ["@mui/material/Container" :default Container]
   ["@mui/material/Box" :default Box]
   ["@mui/material/Checkbox" :default Checkbox]
   ["@mui/material/Toolbar" :default Toolbar]
   ["@mui/material/Menu" :default Menu]
   ["@mui/material/MenuItem" :default MenuItem]
   ["@mui/material/Typography" :default Typography]
   ["@mui/material/AppBar" :default AppBar]
   ["@mui/material/styles" :refer [ThemeProvider createTheme]]))


(def dark-theme
  (createTheme (clj->js {:palette {:primary
                                   {:main "#424242"}
                                   :secondary {:main "#9e9e9e"}}})))


(defn home-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:> Container
      [:> Button "Button"]
      [:> TextField "TextField"]]]))




(defmethod routes/panels :home-panel [] [home-panel])

;; login

(defn login-panel []
  [:div
   [:> Box {:sx {:width 400
                 :backgroundColor "secondary"
                 :display :flex
                 :flexDirection :column
                 :margin "0 auto"
                 :justify-content :center
                 :align-items :center
                 :text-align :center}}

      [:> TextField {:label "Username"
                     :padding "30px"
                     :sx {:m 2}}]

      [:> TextField {:label "Password"
                     :type "password"
                     :sx {:m 2
                          :mt 0}}]
      [:> Button {:sx {:width 100}} "Log in"]]])


(defmethod routes/panels :login-panel [] [login-panel])


(defn my-aliens-panel []
  [:div

   [:h1 "This is my aliens page"]])

(defmethod routes/panels :my-aliens-panel [] [my-aliens-panel])


;; main

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [:> ThemeProvider {:theme dark-theme}
     [:> Box {:sx {:flexGrow 1}}
      [:> AppBar {:position :static
                  :color "primary"}
       [:> Toolbar
        [:> Typography "Men In Black"]
        [:> Button {:color :inherit} "BUTTON"]]]]
     (routes/panels @active-panel)]))

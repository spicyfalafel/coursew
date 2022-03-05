(ns mibui.events
  (:require
   [re-frame.core :as re-frame :refer [trim-v path after dispatch]]
   [mibui.db :as db :refer [default-db set-user-ls remove-user-ls]]
   [ajax.core :refer [json-request-format json-response-format]]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [clojure.string :as str]))


(re-frame/reg-event-db
  ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-fx
  ::navigate
  (fn-traced [_ [_ handler]]
   {:navigate handler}))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn-traced [{:keys [db]} [_ active-panel]]
   {:db (assoc db :active-panel active-panel)}))



;; -- Interceptors --------------------------------------------------------------
;; Every event handler can be "wrapped" in a chain of interceptors. Each of these
;; interceptors can do things "before" and/or "after" the event handler is executed.
;; They are like the "middleware" of web servers, wrapping around the "handler".
;; Interceptors are a useful way of factoring out commonality (across event
;; handlers) and looking after cross-cutting concerns like logging or validation.
;;
;; They are also used to "inject" values into the `coeffects` parameter of
;; an event handler, when that handler needs access to certain resources.
;;
;; Each event handler can have its own chain of interceptors. Below we create
;; the interceptor chain shared by all event handlers which manipulate user.
;; A chain of interceptors is a vector.
;; Explanation of `trim-v` is given further below.
;;
(def set-user-interceptor [(path :user)        ;; `:user` path within `db`, rather than the full `db`.
                           (after set-user-ls) ;; write user to localstore (after)
                           trim-v])            ;; removes first (event id) element from the event vec

(def api-url "http://localhost:8080/api")

(defn endpoint
  "Concat any params to api-url separated by /"
  [& params]
  (str/join "/" (cons api-url params)))



;; -- POST Login @ /api/users/login -------------------------------------------
;;
(re-frame/reg-event-fx                                        ;; usage (dispatch [:login user])
 :login                                              ;; triggered when a users submits login form
 (fn [{:keys [db]} [_ credentials]]                  ;; credentials = {:login ... :password ...}
   {:db         db
    :http-xhrio {:method          :post
                 :uri             (endpoint "users" "login") ;; evaluates to "api/users/login"
                 :params          {:user credentials}    ;; {:user {:email ... :password ...}}
                 :format          (json-request-format)  ;; make sure it's json
                 :response-format (json-response-format {:keywords? true}) ;; json response and all keys to keywords
                 :on-success      [:login-success]       ;; trigger :login-success event
                 :on-failure      [:api-request-error {:request-type :login}]}})) ;; trigger :api-request-error event



(re-frame/reg-event-fx
 :login-success
 ;; The standard set of interceptors, defined above, which we
 ;; use for all user-modifying event handlers. Looks after
 ;; writing user to localStorage.
 ;; NOTE: this chain includes `path` and `trim-v`

 ;  set-user-interceptor)

 ; The event handler function.
 ;; The "path" interceptor in `set-user-interceptor` means 1st parameter is the
 ;; value at `:user` path within `db`, rather than the full `db`.
 ;; And, further, it means the event handler returns just the value to be
 ;; put into `:user` path, and not the entire `db`.
 ;; So, a path interceptor makes the event handler act more like clojure's `update-in`
 ; (fn [{user :db} [{props :user}]]
 ;   {:db         (-> (merge user props)
 ;                    (assoc-in [:loading :login] false))
 ;    :dispatch [:navigate :home]}))
  (+ 1 1))
  ;;(dispatch [::navigate :home]))



;; -- Request Handlers -----------------------------------------------------------
;;

(re-frame/reg-event-db                                         ;; usage (dispatch [:api-request-error {:request-type <error-to-log-as>, :loading <loading-to-turn-off>}])
                                                      ;; :loading is optional and defaults to the :request-type input.
 :api-request-error                                   ;; triggered when we get request-error from the server
 (fn [db [_ {:keys [request-type]} response]] ;; `response` is implicitly conj'ed as the last entry by :http-xhrio event.
   (-> db                                             ;; when we complete a request we need to clean so that our ui is nice and tidy
       (assoc-in [:errors request-type] (get-in response [:response :errors])))))

(ns mibui.events
  (:require
   [re-frame.core :as re-frame :refer [trim-v path after dispatch reg-event-fx
                                       reg-event-db reg-fx inject-cofx]]
   [mibui.db :as db :refer [default-db set-user-ls remove-user-ls]]
   [ajax.core :refer [json-request-format json-response-format]]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [clojure.string :as str]
   [cljs.reader :as rdr]
   [mibui.routes :as routes]
   [clojure.set :as set]))

(def set-user-interceptor [(path :user)
                           (println "set-user-interceptor");; `:user` path within `db`, rather than the full `db`.
                           (after set-user-ls) ;; write user to localstore (after)
                           trim-v])            ;; removes first (event id) element from the event vec

;; After logging out, clean up local-storage so that when a user refreshes
;; the browser she/he is not automatically logged-in, and because it's
;; good practice to clean-up after yourself.
;;
(def remove-user-interceptor [(after remove-user-ls)])

;; -- helpers ---------------------------------------------------------------
;;
(def api-url "http://localhost:8080/api")

(defn endpoint
  "Concat any params to api-url separated by /"
  [& params]
  (str/join "/" (cons api-url params)))

(defn token-header [db]
  (let [token (get-in db [:user :token])]
       {:Token token}))

(defn auth-header
  "Get user token and format for API authorization"
  [db]
  (when-let [token (get-in db [:user :token])]
    [:Authorization (str "Token " token)]))

(defn add-epoch
  "Add :epoch timestamp based on :createdAt field."
  [item]
  (assoc item :epoch (-> item :createdAt rdr/parse-timestamp .getTime)))

(defn index-by
  "Index collection by function f (usually a keyword) as a map"
  [f coll]
  (into {}
        (map (fn [item]
               (let [item (add-epoch item)]
                 [(f item) item])))
        coll))



;; -- Events -------------------------------------------------------------------
;;


(reg-event-fx                                            ;; usage: (dispatch [:initialise-db])
 :initialize-db                                          ;; sets up initial application state

 ;; the interceptor chain (a vector of interceptors)
 [(inject-cofx :local-store-user)]                       ;; gets user from localstore, and puts into coeffects arg

 ;; the event handler (function) being registered
 (fn [{:keys [local-store-user]} _]
   ;; take 2 vals from coeffects. Ignore event vector itself.
   {:db (assoc default-db :user local-store-user)}))     ;; what it returns becomes the new application state

(reg-fx
 :set-url
 (fn [{:keys [url]}]
   (println "set-url")
   (routes/set-token! url)))

(reg-event-fx                                            ;; usage: (dispatch [:set-active-page {:page :home})
 :set-active-page                                        ;; triggered when the user clicks on a link that redirects to another page
 (fn [{:keys [db]} [_ {:keys [page]}]] ;; destructure 2nd parameter to obtain keys
   (let [set-page (assoc db :active-page page)]
     (case page
       ;; -- URL @ "/" --------------------------------------------------------
       :home {:db         set-page}
       ;; -- URL @ "/login" | "/register" | "/settings" -----------------------
       (:login :register :settings) {:db set-page} ;; `case` can group multiple clauses that do the same thing.
                                                   ;; ie., `(:login :register :settings) {:db set-page}` is the same as
                                                   ;;      ```
                                                   ;;      :login {:db set-page}
                                                   ;;      :register {:db set-page}
                                                   ;;      :settings {:db set-page}
                                                   ;;      ```
       :my-aliens {:db set-page
                   :dispatch [:my-aliens]}
       :alien-view {:db set-page}))))
                    ; :dispatch [:alien-view (:)]}))))
; {:db set-page}))))


;; -- POST Login @ /api/users/login -------------------------------------------
;;
(reg-event-fx                                        ;; usage (dispatch [:login user])
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

(reg-event-fx
 :login-success
 ;; The standard set of interceptors, defined above, which we
 ;; use for all user-modifying event handlers. Looks after
 ;; writing user to localStorage.
 ;; NOTE: this chain includes `path` and `trim-v`

 set-user-interceptor
 ; The event handler function.
 ;; The "path" interceptor in `set-user-interceptor` means 1st parameter is the
 ;; value at `:user` path within `db`, rather than the full `db`.
 ;; And, further, it means the event handler returns just the value to be
 ;; put into `:user` path, and not the entire `db`.
 ;; So, a path interceptor makes the event handler act more like clojure's `update-in`
 (fn [cofx event]
   {:db         (first event)
    :dispatch [:set-active-page {:page :home}]}))



;; -- POST Registration @ /api/users ------------------------------------------
;;
(reg-event-fx                                              ;; usage (dispatch [:register-user registration])
 :register-user                                            ;; triggered when a users submits registration form
 (fn [{:keys [db]} [_ registration]]                       ;; registration = {:username ... :email ... :password ...}
   {:db         db
    :http-xhrio {:method          :post
                 :uri             (endpoint "users" "register")     ;; evaluates to "api/users/register"
                 :params          {:user registration}   ;; {:user {:username ... :email ... :password ...}}
                 :format          (json-request-format)  ;; make sure it's json
                 :response-format (json-response-format {:keywords? true}) ;; json response and all keys to keywords
                 :on-success      [:register-user-success] ;; trigger :register-user-success event
                 :on-failure      [:api-request-error {:request-type :register-user}]}})) ;; trigger :api-request-error event

(reg-event-fx
 :register-user-success
 set-user-interceptor

 (fn [user event]
   {:db (first (first event))
    ; (set/rename-keys (first (first event)) {:register_user :id})
    :dispatch [:set-active-page {:page :home}]}))

(reg-event-fx                                            ;; usage (dispatch [:logout])
 :logout
 remove-user-interceptor
 ;; The event handler function removes the user from
 ;; app-state = :db and sets the url to "/".
 (fn [{:keys [db]} _]
   {:db       (dissoc db :user)                          ;; remove user from db
    :dispatch [:set-active-page {:page :home}]}))



;; -- GET My-aliens @ /api/my-aliens ------------------------------------------
;; сделать запрос на получение пришельцев для агента используя {:user {:agent_info_id}}
(reg-event-fx
 :my-aliens
 (fn-traced [{:keys [db]} _]
   {:db         db
    :http-xhrio {:method          :get
                 :uri             (endpoint "my-aliens")
                 :params          {:agent_info_id (get-in db [:user :agent_info_id])}
                 :format          (json-request-format)
                 :response-format (json-response-format {:keywords? true})
                 :on-success      [:my-aliens-success]
                 :on-failure      [:api-request-error {:request-type :my-aliens}]}}))

(reg-event-fx
 :my-aliens-success
 (fn [{:keys [db]} [_ aliens]]
   {:db (assoc db :my-aliens aliens)}))



;; -- GET alien @ /api/my-aliens/id ------------------------------------------
(reg-event-fx
 :alien-view
 (fn-traced [{:keys [db]} [_ alien-id]]
   {:db         db
    :http-xhrio {:method          :get
                 :uri             (endpoint "my-aliens" alien-id)
                 :format          (json-request-format)
                 :response-format (json-response-format {:keywords? true})
                 :on-success      [:view-alien-success]
                 :on-failure      [:api-request-error {:request-type :view-alien}]}}))

(reg-event-fx
 :view-alien-success
 (fn [{:keys [db]} [_ alien]]
   {:db (assoc db :alien-view alien)
    :dispatch [:set-active-page {:page :alien-view}]}))

;; -- Request Handlers -----------------------------------------------------------
;;

(reg-event-db                                         ;; usage (dispatch [:api-request-error {:request-type <error-to-log-as>, :loading <loading-to-turn-off>}])
                                                      ;; :loading is optional and defaults to the :request-type input.
 :api-request-error                                   ;; triggered when we get request-error from the server
 (fn [db [_ {:keys [request-type]} response]] ;; `response` is implicitly conj'ed as the last entry by :http-xhrio event.
   (-> db                                             ;; when we complete a request we need to clean so that our ui is nice and tidy
       (assoc-in [:errors request-type] (get-in response [:response :errors])))))

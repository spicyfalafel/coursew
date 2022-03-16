(ns coursew.server
  (:require
   [immutant.web :as web]
   [compojure.route :as cjr]
   [compojure.core :as compojure]
   ; [cheshire.core :refer [generate-string]]
   [cheshire.generate :refer [JSONable]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie]
   [clojure.string :as str]
   ; [ring.util.response :only [response]]
   ; [buddy.auth.backends.token :refer [token-backend]]
   ; [buddy.auth.middleware :refer [wrap-authentication]]
   [coursew.database :as db]
   [coursew.auth :as auth])
  (:gen-class))


(defmacro if-let*
 ([bindings then]
  `(if-let* ~bindings ~then nil))
 ([bindings then else]
  (if (seq bindings)
    `(if-let [~(first bindings) ~(second bindings)]
       (if-let* ~(drop 2 bindings) ~then ~else)
       ~else)
    then)))
;; LocalDate json encoding
(extend-protocol JSONable
  java.time.LocalDate
  (to-json [dt gen]
    (cheshire.generate/write-string gen (str dt))))


;; 1) заходит агент. надо выдать инфу юзера + айди агента, никнейм
;; 2) заходит пришелец. надо выдать инфу юзера
(defn login [request]
  (let [username (-> request :body :user :username)
        password (-> request :body :user :password)
        user (db/user-by-cred username password)]
    (if (empty? user)
        {:status 404
         :body [(str "no such user " username " " password)]}
        (let [user-id (:user_id user)
              token (auth/generate-signature username password)
              user-token (assoc user :token token)]
          (if-let [agent-info (db/get-if-agent user-id)]
                  {:status 200
                   :body (merge user-token (first agent-info))}
                  (if-let [alien-info (db/get-if-alien user-id)]
                    {:status 200
                     :body (merge user-token (first alien-info))}
                    {:status 404 :body "error"}))))))

(defn register [request]
  (let [username (-> request :body :user :username)
        password (-> request :body :user :password)
        alien (-> request :body :user :alien)]
    (if alien
      (db/register-alien username password)
      (db/register-agent username password))))



;; взять инфу про всех пришельцев, включая то, были для них сегодня репорты или нет
(defn my-aliens [request]
  (let [agent-id (-> request :params (get "agent_info_id") Integer/parseInt)
        reported-ids (db/reports-today agent-id)
        aliens (db/aliens-by-agent-id agent-id)
        set-reported (fn [alien]
                       (let [id (:alien_info_id alien)]
                         (if (contains? reported-ids id)
                          (assoc alien :reported true)
                          (assoc alien :reported false))))

        aliens-rep (map set-reported aliens)]
    {:status 200
     :body aliens-rep}))

(defn view-alien [id]
  (if-let [alien-info (db/alien-by-id (Integer/parseInt id))]
    {:status 200
     :body alien-info}
    {:status 404
     :body (str "no such alien with id " id)}))

(defn report
  [{:keys [params body] :as request}]
  (if-let* [alien-id (-> params :id Integer/parseInt)
            agent-id (-> body :agent_info_id)
            agent-alien-id (db/get-agent-alien agent-id alien-id)
            report-added (db/ins-report! (:report_date body)
                                    (Integer/parseInt (:behavior body))
                                    (:description body)
                                    agent-alien-id)]
    {:status 200
     :body report-added}
    {:status 404
     :body (str "errors " (str request))}))

(defn alien-form [user-id]
  (let [alien-form (db/pending-alien-form (Integer/parseInt user-id))
        skills (db/skills-alien-form (:alien_form_id alien-form))]
    {:status 200
     :body (assoc alien-form :skills skills)}))


(defn get-requests [_]
  (let [ans (db/get-pending-requests)]
    {:status 200
     :body ans}))

(defn set-request-rejected [request-id]
    (db/set-request-rejected (Integer/parseInt request-id))
    {:status 200
     :body "OK"})

(defn save-alien-form [req]
  (let [body (:body req)
        alien-form (select-keys body [:userid :planet_name :visit_purp :staytime :comm])
        skills (str/split (:skills body) #",")
        ids (db/create-visit-request alien-form)]
      (db/form-add-skills (:alien_form_id ids) skills)
      (if ids
        {:status 200
         :body ids}
        {:status 400})))

(defn get-request-and-form [req-id]
  (let [ans (db/request-and-form (Integer/parseInt req-id))
        skills (db/skills-alien-form (:alien_form_id ans))]
    {:status 200
     :body (assoc ans :skills skills)}))

; (get-request-and-form "4")


(defn accept-request [req]
  (let [params (:body req)
        _  (println "PARAMS " params)
        _ (db/accept-request params)]
    {:status 200
     :body "ok"}))

(defn skills-by-user-id [user-id]
  (let [skills (db/skills-by-user-id (Integer/parseInt user-id))]
     {:status 200
      :body skills}))

(compojure/defroutes routes
  (compojure/context "/api" []
    (compojure/context "/requests/:id" [id]
      (compojure/POST "/accept" request (accept-request request)))
    (compojure/GET "/requests/:id" [id] (get-request-and-form id))
    (compojure/POST "/requests/:id/reject" [id] (set-request-rejected id))
    (compojure/GET "/skills/:id" [id] (skills-by-user-id id))
    (compojure/GET "/requests" request (get-requests request))
    ; (compojure/POST "/requests/:id/accept" [id])
    (compojure/context "/users" []
      (compojure/POST "/login" request (login request))
      (compojure/POST "/register" request (register request)))
    (compojure/GET "/my-aliens" request (my-aliens request))
    (compojure/context "/my-aliens/:id" [id]
      (compojure/POST "/report" request (report request))
      (compojure/GET "/" [id] (view-alien id)))
    (compojure/context "/alien-form" []
      (compojure/POST "/" request (save-alien-form request))
      (compojure/GET "/:id" [id] (alien-form id))))
  (cjr/not-found "<h1>Page not found!!!</h1>"))



(def app (-> routes
             wrap-json-response
             (wrap-json-body {:keywords? true :bigdecimals? true})
             wrap-params
             wrap-keyword-params
             (wrap-session {:store (ring.middleware.session.cookie/cookie-store)})
             (wrap-cors :access-control-allow-origin [#"http://localhost:8280"] ;; CORS
                        :access-control-allow-methods [:get :post]
                        :access-control-allow-headers ["Origin" "X-Requested-With" "Content-Type" "Accept"])))



(defn -main [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8080")]
    (println "Starting web server on port" port-str)
    (web/run #'app { :port (Integer/parseInt port-str)})))


(comment
  (def server (-main "--port" "8080"))

  (web/stop server))

(ns coursew.server
  (:require
   [immutant.web :as web]
   [compojure.route :as cjr]
   [compojure.core :as compojure]
   [cheshire.core :refer [generate-string]]
   [cheshire.generate :refer [JSONable]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.util.response :only [response]]
   [coursew.database :as db])
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
        (let [user-id (:id user)]
          (if-let [agent-info (db/get-if-agent user-id)]
                  {:status 200 :body (merge user (first agent-info))}
                  (if-let [alien-info (db/get-if-alien user-id)]
                    {:status 200 :body (merge user (first alien-info))}
                    {:status 404 :body "error"}))))))

(defn register [request]
  (let [username (-> request :body :user :username)
        password (-> request :body :user :password)
        alien (-> request :body :user :alien)]
    (if alien
      (db/register-alien username password)
      (db/register-agent username password))))

(login {:body {:user {:username "myus" :password "myus"}}})
(defn my-aliens [request])

(defn my-requests [request])


(compojure/defroutes routes
  (compojure/POST "/api/users/login" request (login request))
  (compojure/POST "/api/users/register" request (register request))
  (compojure/GET "/api/my-aliens" request (my-aliens request))
  (compojure/GET "/api/my-requests" request (my-requests request))
  (cjr/not-found "<h1>Page not found!!!</h1>"))

(def app (-> routes
             wrap-json-response
             (wrap-json-body {:keywords? true :bigdecimals? true})
             wrap-params
             wrap-keyword-params
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

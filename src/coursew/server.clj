(ns coursew.server
  (:require
   [immutant.web :as web]
   [compojure.route :as cjr]
   [compojure.core :as compojure]
   ;[clojure.data.json :as json]
   [cheshire.core :refer [generate-string]]
   [cheshire.generate :refer [JSONable]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.util.response :only [response]]
   [coursew.database :as db]))


;; LocalDate json encoding
(extend-protocol JSONable
  java.time.LocalDate
  (to-json [dt gen]
    (cheshire.generate/write-string gen (str dt))))

(+ 1 2)

(defn get-patients []
              (generate-string (db/get-patients) {:pretty true}))

(defn add-patient [request]
  (let [patient (:body request)]
    (if-let [ans-map (db/ins-patient! patient)]
      {
       :status 201
       :body ans-map}
      {:status 400
       :body "something went wrong"})))



(defn update-patient [request]
  (let [patient (:body request)
        id (:id :params request)] ;; todo use /patient/:id
    (if-let [ans-map (db/upd-patient! patient)]
      {
       :status 200
       :body ans-map}
      {:status 204
       :body "not found"})))


(defn delete-patient [request]
  (if-let [del-ans (db/del-patient! (Integer/parseInt (-> request :params :id)))]
    {
     :status 200
     :body del-ans}
    {
     :status 404
     :body "not found"}))

(compojure/defroutes routes
  (compojure/GET "/patient" [] {:body (get-patients)})
  (compojure/POST "/patient" request (add-patient request))
  (compojure/PUT "/patient/:id" [id :as request] (update-patient request))
  (compojure/DELETE "/patient/:id" [id :as request] (delete-patient request)),
  (cjr/not-found "<h1>Page not found!!!</h1>"))

(def app (-> routes
             wrap-json-response
             (wrap-json-body {:keywords? true :bigdecimals? true})
             wrap-params
             wrap-keyword-params
             (wrap-cors :access-control-allow-origin [#"http://localhost:8000"] ;; CORS
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

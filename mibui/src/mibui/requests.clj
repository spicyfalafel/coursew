
(ns mibui.requests
  (:require [reagent.core :as r])
  (:require [mibui.json-util :as my-json])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]] [cljs-http.client :as http]))

(def backend-url "http://localhost:8080")
(def agents-endp "/agents")
(def agents (r/atom nil))
(def aliens (r/atom nil))

(defn get-request [endpoint]
  (go (<! (http/get (str backend-url endpoint)
                    {:with-credentials? false
                     :Origin "http://localhost:8000"})))) ;; CORS



(defn read-response [channel]
  (go (let [resp (<! channel)]
        (reset! patients-atom (my-json/parse-string (:body resp))))))

(defn get-agents []
  (-> (get-request agents-endp)
      read-response))

(ns coursew.server-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [cheshire.core :as json]
            ; [coursew.database :as db]
            [coursew.server :as serv]
            ; [clojure.java.jdbc :as jdbc]
            [ring.mock.request :as mock]))

(deftest test-no-such-page
  (let [req {:request-method :get :uri "/"}
        resp (serv/app req)
        {:keys [status]} resp]
    (is (= 404 status))))


(defn contains-many? [mp & ks]
  (every? #(contains? mp %) ks))


(deftest test-get-requests
  (let [resp (serv/app (mock/request :get "/api/requests"))
        body (json/parse-string (:body resp) true)]
    (is (= 200 (:status resp)))
    (is (string? (:body resp)))
    (is (every? #(contains-many? % :request_id :creator_id :date :status :type :username :user_photo)
                body))))

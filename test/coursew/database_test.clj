(ns coursew.database-test
  (:require [coursew.database :as db]
            [clojure.java.io :refer [writer file]]
            [clojure.test :refer [deftest testing is use-fixtures]]))
  ; (:import [java.io File]))


;; сетапим руками тестовую БД: назыавем test-db, кормим sql
;; проверяем:
;; 1) Извлечение даты
;; 2) Вставка даты
;; 3) Желательно каждую функцию в database.clj


(def test-db
  {:dbtype "postgresql"
   :dbname "testdb"
   :host "localhost"
   :user "postgres"
   :password "root"})


(defonce ^:dynamic *tmp-file* nil)

(defn fixt-change-db
  "Фикстура для смены БД на время тестов"
  [t]
  (db/set-db test-db)
  (t)
  (db/reset-db))


(use-fixtures :once
              fixt-change-db)



(deftest test-user-by-cred
  (testing "user by username and password"
    (is (seq (db/user-by-cred "agent" "agent"))))
  (testing "user with bad password"
    (is (nil? (db/user-by-cred "agent" "bad_password")))))

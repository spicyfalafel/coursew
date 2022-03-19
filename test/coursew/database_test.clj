(ns coursew.database-test
  (:require [coursew.database :as db]
            [clojure.java.io :refer [writer file]]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest testing is use-fixtures]]))
  ; (:import [java.io File]))


;; сетапим руками тестовую БД: назыавем test-db, кормим sql
;; проверяем:
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
  (println "db set to test!")
  (t)
  (db/reset-db)
  (println "db reset!"))


(defn fixt-create-temp-table
  "Фикстура для создания таблицы для проверки вставки/чтения даты"
  [t]
  (jdbc/db-do-commands @db/pg-db ["create table testing (datecol date);"])
  (t)
  (jdbc/db-do-commands @db/pg-db ["drop table testing"]))


(use-fixtures :once
              fixt-change-db
              fixt-create-temp-table)


(deftest test-date-insertion
  ; "Проверка извлечения и вставки даты"
  (let [exp-date {:datecol #time/ld "2022-03-03"}]
    (testing "date insert"
      (is exp-date (db/ins! :testing {:datecol (db/parse-date "2022-03-03")}))
      (testing "date query"
        (is exp-date (db/query ["select * from testing"]))))
    (testing "bad date insert"
      (is (thrown? java.time.format.DateTimeParseException
                   (db/ins! :testing {:datecol (db/parse-date "2022-28-28")}))))))


(deftest test-user-by-cred
  (testing "user by username and password"
    (is (seq (db/user-by-cred "agent" "agent"))))
  (testing "user with bad password"
    (is (nil? (db/user-by-cred "agent" "bad_password")))))

(ns coursew.database-test
  (:require [coursew.database :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest testing is use-fixtures]]))


;; сетапим руками тестовую БД: назыавем test-db, кормим sql

(def test-db
  {:dbtype "postgresql"
   :dbname "testdb"
   :host "localhost"
   :user "postgres"
   :password "root"})


(defonce ^:dynamic *test-data* nil)

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

(defn fixt-create-test-data
  [t]

  (binding [*test-data* {:agent {:id 1005
                                 :agent-info-id (:id (first (db/query ["select * from agent_info where user_id = ?" 1005])))
                                 :username "agent"
                                 :password "agent"}
                         :alien {:id 3
                                 :alien-info-id (:id (first (db/query ["select * from alien_info where user_id = ?" 3])))
                                 :username "alien"
                                 :password "alien"}
                         :new-alien {:username "newalien" :password "newalien"}
                         :new-agent {:username "newagent" :password "newagent"}}]
    (t)))

(use-fixtures :once
              fixt-change-db
              fixt-create-temp-table
              fixt-create-test-data)


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
  (let [agent (:agent *test-data*)
        alien (:alien *test-data*)]
    (testing "user by username and password"
      (is (seq (db/user-by-cred (:username agent) (:password agent))))
      (is (seq (db/user-by-cred (:username alien) (:password alien))))))
  (testing "user with bad password"
    (is (nil? (db/user-by-cred "agent" "bad_password")))))


(deftest test-register
  (let [agent (:new-agent *test-data*)
        ag-username (:username agent)
        ag-pass (:password agent)
        alien (:new-alien *test-data*)
        al-username (:username alien)
        al-pass (:password alien)]

    (testing "register alien"
      (db/register-alien al-username al-pass)
      (is (seq (db/user-by-cred al-username al-pass))))
    (testing "register agent"
      (db/register-agent ag-username ag-pass)
      (is (seq (db/user-by-cred ag-username ag-pass))))
    (jdbc/delete! @db/pg-db "\"user\"" ["username =? " ag-username])
    (jdbc/delete! @db/pg-db "\"user\"" ["username =? " al-username])))


(deftest test-get-roles
  (testing "get-roles agent"
    (is (= {:name "AGENT"} (first  (db/get-roles (-> *test-data* :agent :id))))))
  (testing "get-roles alien"
    (is (= {:name "ALIEN"} (first (db/get-roles (-> *test-data* :alien :id))))))
  (testing "get-roles with no such user id"
    (is (empty? (db/get-roles 9999)))))


(deftest test-get-if-alien
  (is (seq (db/get-if-alien (-> *test-data* :alien :id))))
  (testing "agent"
    (is (nil? (db/get-if-alien (-> *test-data* :agent :id)))))
  (testing "bad id"
    (is (nil? (db/get-if-alien 9999)))))


(deftest test-get-if-agent
  (is (seq (db/get-if-agent (-> *test-data* :agent :id))))
  (testing "alien"
    (is (nil? (db/get-if-agent (-> *test-data* :alien :id)))))
  (testing "bad id"
    (is (nil? (db/get-if-agent 9999)))))

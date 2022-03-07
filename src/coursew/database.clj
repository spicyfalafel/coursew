
(ns coursew.database
  (:require
   [honey.sql :as sql]
   [clojure.java.jdbc :as jdbc]
   [honey.sql.helpers :refer :all :as h]
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import [java.sql Timestamp]
           [java.sql Date]
           [java.sql Connection]
           [java.sql PreparedStatement]
           [java.sql ResultSet]
           [java.time.format DateTimeFormatter]
           [java.time LocalDate]
           [java.time Instant]
           [java.io FileWriter]
           [java.io PushbackReader]))



(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))

(defn load-config []
  (load-edn "database-config.edn"))


(def pg-db
  (if-let [config (load-config)]
    config
    {:dbtype "postgresql"
     :dbname "postgres"
     :host "localhost"
     :user "postgres"
     :password "root"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; date insertion fix
(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _ _]
    (.toInstant v))
  java.sql.Date
  (result-set-read-column [v _ _]
    (.toLocalDate v)))

(extend-protocol jdbc/ISQLValue
  java.time.Instant
  (sql-value [v]
    (Timestamp/from v))
  java.time.LocalDate
  (sql-value [v]
    (Date/valueOf v)))


(defmethod print-method java.time.Instant
  [inst out]
  (.write out (str "#time/inst \"" (.toString inst) "\"")))

(defmethod print-dup java.time.Instant
  [inst out]
  (.write out (str "#time/inst \"" (.toString inst) "\"")))

(defmethod print-method LocalDate
  [^LocalDate date ^FileWriter out]
  (.write out (str "#time/ld \"" (.toString date) "\"")))

(defmethod print-dup LocalDate
  [^LocalDate date ^FileWriter out]
  (.write out (str "#time/ld \"" (.toString date) "\"")))

(defn parse-date [string]
  (LocalDate/parse string))

(defn parse-time [string]
  (and string (-> (.parse (DateTimeFormatter/ISO_INSTANT) string)
                  Instant/from)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


; (defn get-patients []
;   (jdbc/query pg-db (sql/format {:select [:patient.firstname
;                                           :patient.lastname
;                                           [:gender.name "gender"]
;                                           :patient.birthdate
;                                           :patient.address
;                                           :patient.polys_id]
;                                  :from [:patient]
;                                  :join [:gender [:= :gender.id :patient.gender_id]]})))
;
; (defn del-patient! [id]
;   (jdbc/delete! pg-db :patient ["id = ?" id]))
;

; (defn replace-birthdate-str [patient]
;   (if-let [birthdate-str (:birthdate patient)]
;     (assoc patient :birthdate (parse-date birthdate-str))
;     patient))
;
; (defn upd-patient! [patient]
;   ;; если в параметре есть birthdate, значит надо его поменять на объект даты
;   ;; если нет, значит менять не надо
;   (jdbc/update! pg-db :patient (replace-birthdate-str patient)
;                 ["id = ?" (:id patient)]))
;
; (defn ins-patient! [patient]
;   (let [pat (assoc patient :birthdate  (parse-date (:birthdate patient)))]
;    (jdbc/insert! pg-db :patient pat)))
;

(defn register-alien [username password]
  (jdbc/query pg-db ["select register_user(?, ?, true)" username password]))

(defn register-agent [username password]
  (jdbc/query pg-db ["select register_user(?, ?, false)" username password]))

(defn alien-info []
  (jdbc/query pg-db (sql/format {:select [:*]

                                 :from [:alien_info]})))

(defn user-by-cred [username passw]
  (jdbc/query pg-db ["select * from \"user\" where username=? and passw_hash=?" username passw]))

(comment
  (user-by-cred "123" "123")
  (user-by-cred "1" "1")

  (register-alien "fsdf" "fsdfsd")
  (first (register-agent "12345fsdfsd6" "123456"))

  (jdbc/query pg-db ["select * from register_user(?, ?)" "2" "2"]))

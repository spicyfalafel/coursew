
(ns coursew.database
  (:refer-clojure :exclude [set into group-by update partition-by filter for])
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

(defn register-alien [username password]
  (jdbc/query pg-db ["select * from register_user(?, ?, true)" username password]))


(defn register-agent [username password]
  (jdbc/query pg-db ["select * from register_agent(?, ?)" username password]))


(defn get-roles [user-id]
  (jdbc/query pg-db ["select r.name from \"user\" u
                      join user_roles ur on u.id = ur.user_id
                      join role r on ur.role_id = r.id where u.id = ?" user-id]))

(defn agent-info [user-id]
  (jdbc/query pg-db ["select id as agent_info_id, nickname from agent_info where user_id = ?" user-id]))


(defn alien-info [user-id]
  (jdbc/query pg-db ["select ai.id as alien_info_id, ai.departure_date, s.name as status from alien_info ai
                     join alien_status s on s.id = ai.alien_status_id where ai.user_id = ?" user-id]))

(defn get-if-agent [user-id]
  (when-let [names (map #(:name %) (get-roles user-id))]
    (when (some #{"AGENT"} names)
      (agent-info user-id))))


(defn get-if-alien [user-id]
  (let [roles (get-roles user-id)]
    (when (some #{"ALIEN"} (map #(:name %) roles))
      (alien-info user-id))))


(defn aliens-by-agent-id [agent-id]
  (list (jdbc/query pg-db ["select al.id as alien_info_id, u.username, u.user_photo, s.name as status,
                           al.personality_id, al.departure_date
                           from agent_info ag
                           join agent_alien aa on ag.id = aa.agent_info_id
                           join alien_info al on aa.alien_info_id = al.id
                           join alien_status s on al.alien_status_id = s.id
                           join \"user\" u on u.id = al.user_id
                           where ag.id=? and s.name = 'ON EARTH'" agent-id])))

(defn alien-by-id [alien-info-id]
  (first (jdbc/query pg-db ["select u.username, u.user_photo,
                           s.name as status,
                           p.first_name, p.second_name, p.age, p.person_photo,
                           l.city, l.country,
                           prof.name as profession_name,
                           al.id as alien_info_id
                           from alien_info al
                           join alien_personality p on p.id = al.personality_id
                           join location l on l.id = p.location_id
                           join \"user\" u on al.user_id = u.id
                           join alien_status s on al.alien_status_id = s.id
                           join profession prof on prof.id = p.profession_id
                           where al.id = ? and s.name = 'ON EARTH'" alien-info-id])))
(alien-by-id 2)
(defn user-by-cred [username passw]
  (first
    (jdbc/query pg-db ["select id, username, user_photo from \"user\" where username=? and passw_hash=?" username passw])))


(comment
  (user-by-cred "123" "123")
  (user-by-cred "1" "1")

  (register-alien "fsdf" "fsdfsd")
  (first (register-agent "12345fsdfsd6" "123456"))

  (jdbc/query pg-db ["select * from register_user(?, ?)" "2" "2"]))

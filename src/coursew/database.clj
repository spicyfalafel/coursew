
(ns coursew.database
  (:refer-clojure :exclude [group-by update partition-by filter for])
  (:require
   [honey.sql :as sql]
   [clojure.java.jdbc :as jdbc]
   [honey.sql.helpers :as h]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import [java.sql Timestamp]
           [java.sql Date]
           [java.time.format DateTimeFormatter]
           [java.time LocalDate]
           [java.time Instant]
           [java.io FileWriter]))



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


(def default-config
  {:dbtype "postgresql"
   :dbname "postgres"
   :host "localhost"
   :user "postgres"
   :password "root"})

(def config-filename "database-config.edn")


(def pg-db
  (atom ;; хочу менять в тестах базу данных, ничего умнее не придумалы
    (if-let [config (load-edn config-filename)]
      config
      default-config)))

(defn set-db [db-config]
  (reset! pg-db db-config))

(defn reset-db []
  (reset! pg-db default-config))

(def query (partial jdbc/query @pg-db))

(def ins! (partial jdbc/insert! @pg-db))

;;---------------------------date insertion fix---------------------------------
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
;;------------------------------------------------------------------------------

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

;;----------------user----------------------------------------------------------


(defn user-by-cred [username passw]
  (first
    (query ["select id as user_id, username, user_photo from \"user\" where username=? and passw_hash=?" username passw])))


(defn get-roles [user-id]
  (query ["select r.name from \"user\" u
                      join user_roles ur on u.id = ur.user_id
                      join role r on ur.role_id = r.id where u.id = ?" user-id]))


(defn register-alien [username password]
  (query ["select * from register_alien(?, ?)" username password]))

(defn alien-by-id [alien-info-id]
  (first (query ["select u.username, u.user_photo,
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


(defn form-by-request-id [request-id]
  (query ["select r.id as request_id, r.creator_id, r.create_date,
                     t.name as request_type, s.name as status,
                     f.planet_id, f.visit_purpose, f.stay_time, f.comment,
                     p.name as planet_name, p.race
                     from request r
                     join request_type t on r.type_id = t.id
                     join request_status s on s.id = r.status_id
                     join alien_form f on r.alien_form_id = f.id
                     join planet p on p.id = f.planet_id
                     where r.id=? and s.name = 'PENDING' and t.name = 'VISIT'" request-id]))

(defn alien-info [user-id]
  (query ["select ai.id as alien_info_id, ai.departure_date, s.name as status from alien_info ai
                     join alien_status s on s.id = ai.alien_status_id where ai.user_id = ?" user-id]))

(defn get-if-alien [user-id]
  (let [roles (get-roles user-id)]
    (when (some #{"ALIEN"} (map #(:name %) roles))
      (alien-info user-id))))

(defn pending-alien-form [user_id]
  (first (query ["select f.id as alien_form_id, f.user_id, p.name as planet_name, f.visit_purpose as visit_purp,
                      f.stay_time as staytime, f.comment as comm from alien_form f
                      join request r on f.id = r.alien_form_id
                      join request_status s on s.id = r.status_id
                      join planet p on p.id = f.planet_id
                      where s.name = 'PENDING' and f.user_id = ?" user_id])))


(defn create-visit-request [{:keys [userid planet_name visit_purp staytime comm]}]
  (first (query ["select * from create_visit_request(?, ?, ?, ?, ?)"]
             userid planet_name visit_purp staytime comm)))

(defn form-add-skills [form-id skills]
  (let [skills-arg (str "'{" (str/join "," skills)  "}'")]
    (query [(str "select from insert_skill_in_alien_form(?, " skills-arg  ")") (int form-id)])))



(defn register-agent [username password]
  (query ["select * from register_agent(?, ?)" username password]))


(defn reports-today [agent-id]
  (into #{} (map #(:alien_info_id %) (query ["select alien_info_id from agent_alien aa
                      join tracking_report t on aa.id = t.agent_alien_id
                      where agent_info_id = ? and t.report_date = current_date" agent-id]))))


(defn ins-report! [report-date behavior description agent-alien-id]
  (ins! :tracking_report {:report_date (parse-date report-date)
                          :behavior behavior
                          :description description
                          :agent_alien_id agent-alien-id}))


(defn agent-info [user-id]
  (query ["select id as agent_info_id, nickname from agent_info where user_id = ?" user-id]))



(defn get-if-agent [user-id]
  (when-let [names (map #(:name %) (get-roles user-id))]
    (when (some #{"AGENT"} names)
      (agent-info user-id))))



(defn aliens-by-agent-id [agent-id]
  (into #{} (query ["select al.id as alien_info_id, u.username, u.user_photo, s.name as status,
                           al.personality_id, al.departure_date
                           from agent_info ag
                           join agent_alien aa on ag.id = aa.agent_info_id
                           join alien_info al on aa.alien_info_id = al.id
                           join alien_status s on al.alien_status_id = s.id
                           join \"user\" u on u.id = al.user_id
                           where ag.id=? and s.name = 'ON EARTH'" agent-id])))



(defn get-agent-alien [agent-id alien-id]
  (:id (first (query ["select id
                      from agent_alien aa
                      where alien_info_id = ? and agent_info_id = ?" alien-id agent-id]))))


(defn get-pending-requests []
  (into [] (query ["select r.id as request_id, r.creator_id, date(r.create_date),
                               s.name as status, t.name as type, u.username, u.user_photo
                               from request r
                               join request_type t on r.type_id = t.id
                               join request_status s on s.id = r.status_id
                               join \"user\" u on r.creator_id = u.id
                               where s.name = 'PENDING' and t.name = 'VISIT'
                               order by r.id"])))

(defn request-and-form [request-id]
  (first (query  ["select r.id as request_id, r.creator_id, date(r.create_date),
                  t.name as request_type, s.name as status,
                  f.id as alien_form_id,
                  f.planet_id, f.visit_purpose, f.stay_time, f.comment,
                  p.name as planet_name, p.race,
                  u.username, u.user_photo
                  from request r
                  join request_type t on r.type_id = t.id
                  join request_status s on s.id = r.status_id
                  join alien_form f on r.alien_form_id = f.id
                  join planet p on p.id = f.planet_id
                  join \"user\" u on r.creator_id = u.id
                  where r.id=? and s.name = 'PENDING' and t.name = 'VISIT'" request-id])))


(defn set-request-rejected [request-id]
  (query ["select reject_request(?)" request-id]))

(defn skills-alien-form [form-id]
  (map (comp :name) (query ["select name from alien_form f
                     join skill_in_alien_form siaf on f.id = siaf.alien_form_id
                     join skill s on siaf.skill_id = s.id
                     where f.id = ?" form-id])))


(defn skills-by-user-id [user-id]
  (query ["select * from get_professions_by_user_id(?)" user-id]))

; =)
(defn accept-request [{:keys [request_id creatorid executorid firstname secondname
                              agearg professionname cityname countryname personphoto]}]
  (query ["select from accept_request(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
          request_id creatorid executorid firstname secondname
          agearg professionname cityname countryname personphoto])

  (comment
     (user-by-cred "123" "123")
     (user-by-cred "1" "1")))

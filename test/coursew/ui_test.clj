(ns coursew.ui-test
  (:require [etaoin.api :refer [firefox go wait-visible fill quit click
                                get-element-text delete-cookies
                                perform-actions make-key-input with-key-down
                                add-key-press wait query visible?]]
            [etaoin.keys :as k]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [coursew.database :as db]
            [clojure.java.jdbc :as jdbc]))


(defonce ^:dynamic *d* nil)

(def front-url "http://localhost:8280")

(defn fixt-setup-driver
  "Фикстура создает драйвер и выходит после тестов"
  [t]
  (binding [*d* (firefox)]
    (t)
    (quit *d*)))


(defn fixt-delete-cookies
  [t]
  (delete-cookies *d*)
  (t))

(defn fixt-logout
  [t]
  (t)
  (try
      (when (visible? *d* {:tag :a :fn/text "Log out"})
        (click *d* {:tag :a :fn/text "Log out"})) ;; хз как не повторять
      (wait-visible *d* {:tag :div :id :login-div})
    (catch Exception e (str "Can't logout: " (.getMessage e)))))

(use-fixtures :once
              fixt-setup-driver)

(use-fixtures :each
              fixt-delete-cookies
              fixt-logout)


(def reframe-10x (atom true))

(defn home []
  (go *d* front-url)
  (wait 2)
  (when @reframe-10x
      (reset! reframe-10x false)
      (println "RESET")
      (perform-actions *d* (-> (make-key-input)
                               (with-key-down k/control-left (add-key-press "h")))))

  (wait-visible *d* [{:id :login}]))

(deftest test-sign-in
  (testing "Sign in page is visible"
    (home)
    (is (query *d* {:tag :div :id :login-div})))
  (testing "Insert username/password"
    (fill *d* {:tag :input :id :login} "agent")
    (fill *d* {:tag :input :id :passw} "agent"))
  (testing "Click OK"
    (click *d* {:tag :button :fn/text "OK"})
    (wait-visible *d* {:tag :div :id :my-aliens}))
  (testing "Header contains username"
    (is (= (str "AGENT agent") (get-element-text *d* {:tag :li :id :nav-username})))))


(deftest test-sign-up
  (testing "Go to Sign up page"
    (doto *d*
      (go front-url)
      (wait-visible {:tag :nav})
      (click {:tag :a :fn/text "Sign up"})
      (wait-visible {:tag :input :id :username})))
  (let [name "temp-agent" pass name]
    (testing "Enter agent username password"
      (fill *d* {:tag :input :id :username} name)
      (fill *d* {:tag :input :id :pass} pass))
    (testing "Click OK"
      (click *d* {:tag :button :fn/text "OK"})
      (wait-visible *d* {:tag :div :id :my-aliens}))
    (testing "Check if new user is in database"
      (is (seq (db/user-by-cred name pass))))
    (jdbc/delete! @db/pg-db "\"user\"" ["username =? " name])))

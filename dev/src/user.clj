(ns user
  (:require [clj-http.client :as http]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.auth0 :as auth0])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(def db-container (atom nil))

(ig-repl/set-prep!
  (fn []
    (when-not @db-container
      (reset! db-container (doto (PostgreSQLContainer. "postgres:latest") ;; Full reference to PostgreSQLContainer
                             (.withDatabaseName "test_db")
                             (.withUsername "user")
                             (.withPassword "password")
                             (.start))))
    (-> "dev/resources/config.edn"
        slurp
        ig/read-string
        (assoc-in [:db/postgres :jdbc-url] (str (.getJdbcUrl @db-container)
                                                "&user=" (.getUsername @db-container)
                                                "&password=" (.getPassword @db-container))))))

(def go ig-repl/go)
(defn halt []
  (ig-repl/halt)
  (.stop @db-container))
(defn reset []
  (ig-repl/reset)
  (.stop @db-container))
(defn reset-all []
  (ig-repl/reset-all)
  (.stop @db-container))

(def app (-> state/system :pigeon-scoops-backend/app))
(def db (-> state/system :db/postgres))
(def auth (-> state/system :auth/auth0))
(def token (atom nil))
(def test-user (atom nil))

(defn get-test-token [{:keys [test-client-id username password]}]
  (->> {:content-type  :json
        :cookie-policy :standard
        :body          (m/encode "application/json"
                                 {:client_id  test-client-id
                                  :audience   "https://pigeon-scoops.us.auth0.com/api/v2/"
                                  :grant_type "password"
                                  :username   username
                                  :password   password
                                  :scope      "openid profile email"})}
       (http/post "https://pigeon-scoops.us.auth0.com/oauth/token")
       (m/decode-response-body)
       :access_token))

(comment
  (jdbc/execute-one! db ["SELECT table_name FROM information_schema.tables"])
  (jdbc/execute-one! db ["select * from pg_stat_statements_info"])
  (sql/find-by-keys db :recipe {:public false})
  (let [auth (:auth/auth0 state/system)
        username "repl-user@pigeon-scoops.com"
        password (:test-password auth)
        create-response (auth0/create-user! auth
                                            {:connection "Username-Password-Authentication"
                                             :email      username
                                             :password   password})]
    (reset! test-user {:username username
                       :password password
                       :uid      (:user_id create-response)})
    (reset! token (get-test-token (conj auth @test-user))))

  (go)
  (reset! db-container nil)
  (halt)
  (reset)
  (reset-all)
  (parse-postgres-uri))

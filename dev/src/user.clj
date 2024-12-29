(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [pigeon-scoops-backend.auth0 :as auth0]
            [pigeon-scoops-backend.server])
  (:import (org.flywaydb.core Flyway)))

(ig-repl/set-prep!
  (fn []
    (-> "dev/resources/config.edn"
        slurp
        ig/read-string)))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(def app (-> state/system :pigeon-scoops-backend/app))
(def db (-> state/system :db/postgres))
(def auth (-> state/system :auth/auth0))
(def token (atom nil))
(def test-user (atom nil))

(comment
  (reset! token (auth0/get-test-token (merge auth {:username "repl-user@pigeon-scoops.com"
                                                   :password (:test-password auth)})))
  (jdbc/execute-one! db ["SELECT table_name FROM information_schema.tables"])
  (jdbc/execute-one! db ["select * from pg_stat_statements_info"])
  (-> (Flyway/configure)
      (.dataSource (:connectable db))
      (.locations (into-array String ["classpath:db/migrations"]))
      (.load)
      (.migrate))
  (sql/find-by-keys db :recipe {:public true})

  (go)
  (halt)
  (reset)
  (reset-all)
  (parse-postgres-uri))

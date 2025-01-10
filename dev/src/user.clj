(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [next.jdbc.sql :as sql])
  (:import (org.testcontainers.containers PostgreSQLContainer)
           (java.util UUID)))

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
  (when @db-container
    (.stop @db-container)))
(defn reset []
  (ig-repl/reset)
  (when @db-container
    (.stop @db-container)))
(defn reset-all []
  (ig-repl/reset-all)
  (when @db-container
    (.stop @db-container)))

(comment
  (sql/insert! (:db/postgres state/system) :user-order {:id (UUID/randomUUID) :note "asdf" :user-id ""})
  (go)
  (halt)
  (reset)
  (reset-all)
  (parse-postgres-uri))

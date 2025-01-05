(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as ig-repl])
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

(comment
  (go)
  (halt)
  (reset)
  (reset-all)
  (parse-postgres-uri))

(ns pigeon-scoops-backend.config
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as njc]
            [next.jdbc.result-set :as rs])
  (:import (com.zaxxer.hikari HikariDataSource)
           (java.sql Array)))

(defn load-config [config-file]
  (-> config-file
      (slurp)
      (ig/read-string)))

(defn init-system [config]
  (-> config
      (ig/expand)
      (ig/init)))

(defmethod ig/expand-key :db/postgres [k config]
  {k (merge config (when-some [jdbc-url (env :jdbc-database-url)]
                     {:jdbc-url jdbc-url}))})

(defmethod ig/init-key :db/postgres [_ {:keys [jdbc-url]}]
  (println "\n Configured DB")
  (extend-protocol rs/ReadableColumn
    Array
    (read-column-by-label [v _]
      (vec (.getArray v)))                                  ; Convert the SQL array into a vector
    (read-column-by-index [v _ _]
      (vec (.getArray v))))                                 ; Convert the SQL array into a vector
  (jdbc/with-options
    (njc/->pool HikariDataSource {:jdbcUrl jdbc-url})
    jdbc/snake-kebab-opts))

(defmethod ig/halt-key! :db/postgres [_ config]
  (.close ^HikariDataSource (:connectable config)))

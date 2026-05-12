(ns pigeon-scoops-backend.db
  (:require [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as njc]
            [next.jdbc.result-set :as rs])
  (:import (com.zaxxer.hikari HikariDataSource)
           (java.sql Array)))

(defmethod ig/expand-key :db/postgres [k config]
  {k (merge config (when-some [jdbc-url (env :jdbc-database-url)]
                     {:jdbc-url jdbc-url}))})

(defmethod ig/init-key :db/postgres [_ {:keys [jdbc-url]}]
  (log/info "Configured DB")
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

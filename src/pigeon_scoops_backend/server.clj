(ns pigeon-scoops-backend.server
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as njc]
            [next.jdbc.result-set :as rs]
            [pigeon-scoops-backend.router :as router]
            [ring.adapter.jetty :as jetty])
  (:import (com.zaxxer.hikari HikariDataSource)
           (java.sql Array)
           (org.eclipse.jetty.server Server)
           (org.flywaydb.core Flyway)))

(defn app [env]
  (router/routes env))

(defmethod ig/expand-key :server/jetty [k config]
  {k (merge config (when-some [port (env :port)]
                     {:port (Integer/parseInt port)}))})

(defmethod ig/expand-key :db/postgres [k config]
  {k (merge config (when-some [jdbc-url (env :jdbc-database-url)]
                     {:jdbc-url jdbc-url}))})

(defmethod ig/expand-key :auth/auth0 [k config]
  {k (merge config (cond-> {}
                           (env :test-client-id) (conj {:test-client-id (env :test-client-id)})
                           (env :management-client-id) (conj {:management-client-id (env :management-client-id)})
                           (env :management-client-secret) (conj {:management-client-secret (env :management-client-secret)})))})

(defmethod ig/init-key :server/jetty [_ {:keys [handler port]}]
  (println "\n Server running on port" port)
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/init-key :pigeon-scoops-backend/app [_ config]
  (println "\n Starting app")
  (app config))

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

(defmethod ig/init-key :auth/auth0 [_ config]
  config)

(defmethod ig/init-key :db/migration [_ {:keys [jdbc-url]}]
  (println "\n Migrating database")
  (-> (Flyway/configure)
      (.dataSource (:connectable jdbc-url))
      (.locations (into-array String ["classpath:db/migrations"]))
      (.load)
      (.migrate)))

(defmethod ig/halt-key! :server/jetty [_ ^Server jetty]
  (.stop jetty))

(defmethod ig/halt-key! :db/postgres [_ config]
  (.close ^HikariDataSource (:connectable config)))

(defn -main
  [config-file & _]
  (let [config (-> config-file
                   slurp
                   ig/read-string)]
    (-> config
        ig/expand
        ig/init)))

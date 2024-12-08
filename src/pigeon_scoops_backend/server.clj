(ns pigeon-scoops-backend.server
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [pigeon-scoops-backend.router :as router]
            [ring.adapter.jetty :as jetty])
  (:import (org.eclipse.jetty.server Server)))

(defn app [env]
  (clojure.pprint/pprint env)
  (router/routes env))

(defmethod ig/expand-key :server/jetty [k config]
  {k (merge config {:port (Integer/parseInt (env :port))})})

(defmethod ig/expand-key :db/postgres [k config]
  {k (merge config {:jdbc-url (env :jdbc-database-url)})})

(defmethod ig/init-key :server/jetty [_ {:keys [handler port]}]
  (println "\n Server running on port" port)
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/init-key :pigeon-scoops-backend/app [_ config]
  (println "\n Starting app")
  (app config))

(defmethod ig/init-key :db/postgres [_ {:keys [jdbc-url]}]
  (println "\n Configured DB")
  (jdbc/with-options jdbc-url jdbc/snake-kebab-opts))

(defmethod ig/init-key :auth/auth0 [_ config]
  config)

(defmethod ig/halt-key! :server/jetty [_ ^Server jetty]
  (.stop jetty))

(defn -main
  "I don't do a whole lot ... yet."
  [config-file & args]
  (let [config (-> config-file
                   slurp
                   ig/read-string)]
    (-> config
        ig/expand
        ig/init)))

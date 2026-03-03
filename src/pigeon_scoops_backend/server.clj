(ns pigeon-scoops-backend.server
  (:gen-class)
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]
            [pigeon-scoops-backend.db :as config]
            [pigeon-scoops-backend.router :as router]
            [ring.adapter.jetty :as jetty])
  (:import (org.eclipse.jetty.server Server)))

(defmethod ig/expand-key :server/jetty [k config]
  {k (merge config (when-some [port (env :port)]
                     {:port (Integer/parseInt port)}))})

(defmethod ig/init-key :server/jetty [_ {:keys [handler port]}]
  (println "\nServer running on port" port)
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/init-key :server/routes [_ config]
  (println "\nStarting app")
  (router/routes config))

(defmethod ig/halt-key! :server/jetty [_ ^Server jetty]
  (.stop jetty))

(defn -main
  [config-file & _]
  (-> config-file
      (config/load-config)
      (config/init-system)))

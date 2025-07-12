(ns pigeon-scoops-backend.server
  (:require
    [pigeon-scoops-backend.config :as config]))



(defn -main
  [config-file & _]
  (-> config-file
      (config/load-config)
      (config/init-system)))

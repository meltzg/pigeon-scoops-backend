(ns pigeon-scoops-backend.db-tasks
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :as cli]
            [integrant.core :as ig]
            [pigeon-scoops-backend.config :as config])
  (:import (org.flywaydb.core Flyway)))


(def options
  [["-c" "--config-file CONFIG_FILE" "Config File"
    :validate [#(.exists (io/file %))]]
   ["-t" "--task TASK" "DB Task"
    :parse-fn keyword]])


(defmethod ig/init-key :tasks/migration [_ {:keys [jdbc-url]}]
  (fn []
    (println "\n Migrating database")
    (-> (Flyway/configure)
        (.dataSource (:connectable jdbc-url))
        (.locations (into-array String ["classpath:db/migrations"]))
        (.load)
        (.migrate))))


(defn -main
  [& args]
  (let [opts (cli/parse-opts args options)]
    (pprint opts)
    (if (:errors opts)
      (println (:errors opts))
      (let [system (-> opts
                       :options
                       :config-file
                       (config/load-config)
                       (config/init-system))
            task (->> opts
                      :options
                      :task
                      (get system))]
        (task)
        (ig/halt! system)))))

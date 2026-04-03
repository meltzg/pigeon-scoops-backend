(ns pigeon-scoops-backend.db-tasks
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [integrant.core :as ig]
            [pigeon-scoops-backend.db :as config]
            [pigeon-scoops-backend.menu.db :as menu-db]
            [pigeon-scoops-backend.user-order.db :as order-db]
            [pigeon-scoops-backend.utils :refer [end-time with-connection]])
  (:import (java.time ZonedDateTime)
           (org.flywaydb.core Flyway)))

(def options
  [["-c" "--config-file CONFIG_FILE" "Config File"
    :validate [#(.exists (io/file %))]]
   ["-t" "--task TASK" "DB Task"
    :parse-fn keyword]])

(defmethod ig/init-key :db-tasks/migration [_ {:keys [jdbc-url]}]
  (fn []
    (println "\nMigrating database")
    (-> (Flyway/configure)
        (.dataSource (:connectable jdbc-url))
        (.locations (into-array String ["classpath:db/migrations"]))
        (.load)
        (.migrate))))

(defmethod ig/init-key :db-tasks/accept-orders [_ {:keys [jdbc-url]}]
  (fn []
    (println "\nAccepting orders on expired menus")
    (with-connection
      jdbc-url
      (fn [db]
        (let [expired-menus (->> (menu-db/find-all-menus db)
                                 (filter #(> (.toEpochSecond (ZonedDateTime/now))
                                             (.getEpochSecond (.toInstant (:menu/end-time %))))))
              recipes-to-accept (->> (menu-db/find-active-menu-items db)
                                     (filter #((set (map :menu/id expired-menus)) (:menu-item/menu-id %)))
                                     (mapv :menu-item/recipe-id))]
          (when (seq recipes-to-accept)
            (apply (partial order-db/bulk-status-update! db {:status/submitted :status/in-progress
                                                             :status/draft :status/canceled})
                   recipes-to-accept))
          (dorun (->> expired-menus
                      (remove :menu/repeats)
                      (map #(menu-db/update-menu! db (assoc % :menu/active false)))))
          (dorun (->> expired-menus
                      (filter :menu/repeats)
                      (map #(menu-db/update-menu! db (assoc % :menu/end-time (end-time (:menu/duration %)
                                                                                       (:menu/duration-type %))))))))))))

(defn -main
  [& args]
  (let [opts (cli/parse-opts args options)]
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

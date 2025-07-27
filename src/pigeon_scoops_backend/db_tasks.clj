(ns pigeon-scoops-backend.db-tasks
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [pigeon-scoops-backend.config :as config]
            [pigeon-scoops-backend.menu.db :as menu-db]
            [pigeon-scoops-backend.user-order.db :as order-db]
            [pigeon-scoops-backend.utils :refer [with-connection end-time]])
  (:import (java.time ZonedDateTime)
           (org.flywaydb.core Flyway)))


(def options
  [["-c" "--config-file CONFIG_FILE" "Config File"
    :validate [#(.exists (io/file %))]]
   ["-t" "--task TASK" "DB Task"
    :parse-fn keyword]])


(defmethod ig/init-key :tasks/migration [_ {:keys [jdbc-url]}]
  (fn []
    (println "\nMigrating database")
    (-> (Flyway/configure)
        (.dataSource (:connectable jdbc-url))
        (.locations (into-array String ["classpath:db/migrations"]))
        (.load)
        (.migrate))))


(defmethod ig/init-key :tasks/accept-orders [_ {:keys [jdbc-url]}]
  (fn []
    (println "\nAccepting orders on expired menus")
    (with-connection
      jdbc-url
      (fn [conn-opts]
        (let [expired-menus (->> (menu-db/find-all-menus conn-opts)
                                 (filter #(> (ZonedDateTime/now) (:menu/end-time %)))
                                 (map :menu/id))
              recipes-to-accept (->> (menu-db/find-active-menu-items conn-opts)
                                     (filter #(some (set expired-menus) (:menu-item/menu-id %)))
                                     (map :menu-item/recipe-id))]
          (jdbc/with-transaction
            [tx conn-opts]
            (apply (partial order-db/accept-orders! tx) recipes-to-accept)
            (dorun (->> expired-menus
                        (filter :menu/repeats)
                        (map #(menu-db/update-menu! tx {:id % :active false}))))
            (dorun (->> expired-menus
                        (remove :menu/repeats)
                        (map #(menu-db/update-menu! tx {:id %
                                                        :end-time (end-time (:menu/duration %)
                                                                            (:menu/duration-type %))}))))))))))



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

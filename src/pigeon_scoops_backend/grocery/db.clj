(ns pigeon-scoops-backend.grocery.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.utils :refer [db-str->keyword
                                                 keyword->db-str
                                                 with-connection]]))

(defn find-all-grocery-units [db grocery-id]
  (map #(db-str->keyword %
                         :grocery-unit/unit-common-type
                         :grocery-unit/unit-mass-type
                         :grocery-unit/unit-volume-type)
       (sql/find-by-keys db :grocery-unit {:grocery-id grocery-id})))

(defn find-all-groceries
  ([db]
   (find-all-groceries db false))
  ([db include-deleted?]
   (map #(db-str->keyword % :grocery/department)
        (sql/find-by-keys db :grocery (if include-deleted?
                                        :all
                                        {:deleted false})))))

(defn find-grocery-by-id [db grocery-id]
  (with-connection
    db
    (fn [conn-opts]
      (let [[grocery] (sql/find-by-keys conn-opts :grocery {:id grocery-id})
            units (find-all-grocery-units conn-opts grocery-id)]
        (when (seq grocery)
          (-> grocery
              (db-str->keyword :grocery/department)
              (assoc :grocery/units units)))))))

(defn insert-grocery! [db grocery]
  (sql/insert! db :grocery (keyword->db-str grocery :department)))

(defn update-grocery! [db grocery]
  (-> (sql/update! db :grocery (keyword->db-str grocery :department) (select-keys grocery [:id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-grocery! [db grocery-id]
  (-> (sql/update! db :grocery {:deleted true} {:id grocery-id})
      ::jdbc/update-count
      (pos?)))

(defn insert-grocery-unit! [db unit]
  (sql/insert! db :grocery-unit (keyword->db-str unit
                                                 :unit-common-type
                                                 :unit-mass-type
                                                 :unit-volume-type)))

(defn update-grocery-unit! [db unit]
  (-> unit
      (keyword->db-str
        :unit-common-type
        :unit-mass-type
        :unit-volume-type)
      (#(sql/update! db :grocery-unit %
                     (select-keys % [:id])))
      ::jdbc/update-count
      (pos?)))

(defn delete-grocery-unit! [db unit]
  (-> (sql/delete! db :grocery-unit unit)
      ::jdbc/update-count
      (pos?)))

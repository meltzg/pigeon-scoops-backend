(ns pigeon-scoops-backend.order.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.utils :refer [db-str->keyword
                                                 keyword->db-str]]))

(defn find-all-order-items [db order-id]
  (map #(db-str->keyword (into {} (remove (comp nil? val) %))
                         :order-item/unit-common-type
                         :order-item/unit-mass-type
                         :order-item/unit-volume-type)
       (sql/find-by-keys db :order-item {:order-id order-id})))

(defn find-all-orders [db user-id]
  (map #(db-str->keyword % :order/department)
       (sql/find-by-keys db :order {:user-id user-id})))

(defn find-order-by-id [db order-id]
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          [order] (sql/find-by-keys conn-opts :order {:id order-id})
          items (find-all-order-items conn-opts order-id)]
      (when (seq order)
        (-> order
            (db-str->keyword :order/department)
            (assoc :order/items items))))))

(defn insert-order! [db order]
  (sql/insert! db :order (keyword->db-str order :department)))

(defn update-order! [db order]
  (-> (sql/update! db :order (keyword->db-str order :department) (select-keys order [:id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-order! [db order-id]
  (-> (sql/delete! db :order {:id order-id})
      ::jdbc/update-count
      (pos?)))


(defn insert-order-item! [db unit]
  (sql/insert! db :order-item (keyword->db-str unit
                                               :unit-common-type
                                               :unit-mass-type
                                               :unit-volume-type)))


(defn update-order-item! [db unit]
  (-> unit
      (keyword->db-str
        :unit-common-type
        :unit-mass-type
        :unit-volume-type)
      (#(sql/update! db :order-item %
                     (select-keys % [:id])))
      ::jdbc/update-count
      (pos?)))

(defn delete-order-item! [db unit]
  (-> (sql/delete! db :order-item unit)
      ::jdbc/update-count
      (pos?)))

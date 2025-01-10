(ns pigeon-scoops-backend.user-order.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.utils :refer [db-str->keyword
                                                 keyword->db-str]]))

(defn find-all-order-items [db order-id]
  (map #(db-str->keyword (into {} (remove (comp nil? val) %))
                         :order-item/status)
       (sql/find-by-keys db :order-item {:order-id order-id})))

(defn find-all-orders [db user-id]
  (map #(db-str->keyword % :user-order/amount-unit :user-order/status)
       (sql/find-by-keys db :user-order {:user-id user-id})))

(defn find-order-by-id [db order-id]
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          [order] (sql/find-by-keys conn-opts :user-order {:id order-id})
          items (find-all-order-items conn-opts order-id)]
      (when (seq order)
        (-> order
            (db-str->keyword :user-order/amount-unit :user-order/status)
            (assoc :user-order/items items))))))

(defn insert-order! [db order]
  (sql/insert! db :user-order (keyword->db-str order :amount-unit :status)))

(defn update-order! [db order]
  (-> (sql/update! db :user-order (keyword->db-str order :amount-unit :status) (select-keys order [:id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-order! [db order-id]
  (-> (sql/delete! db :user-order {:id order-id})
      ::jdbc/update-count
      (pos?)))

(defn insert-order-item! [db item]
  (sql/insert! db :order-item (keyword->db-str item :status)))

(defn update-order-item! [db item]
  (-> item
      (keyword->db-str :status)
      (#(sql/update! db :order-item %
                     (select-keys % [:id])))
      ::jdbc/update-count
      (pos?)))

(defn delete-order-item! [db item]
  (-> (sql/delete! db :order-item item)
      ::jdbc/update-count
      (pos?)))

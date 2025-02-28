(ns pigeon-scoops-backend.user-order.db
  (:require [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.utils :refer [db-str->keyword
                                                 with-connection
                                                 keyword->db-str]]))

(defn find-all-order-items [db order-id]
  (map #(db-str->keyword (into {} (remove (comp nil? val) %))
                         :order-item/status :order-item/amount-unit)
       (sql/query db (-> (h/select :order-item/* :recipe/name)
                         (h/from :order-item)
                         (h/left-join :recipe [:= :order-item/recipe-id :recipe/id])
                         (h/where [:= :order-item/order-id order-id])
                         (hsql/format)))))

(defn find-all-orders
  ([db user-id]
   (find-all-orders db user-id false))
  ([db user-id include-deleted?]
   (map #(db-str->keyword % :user-order/amount-unit :user-order/status)
        (sql/find-by-keys db :user-order (merge {:user-id user-id}
                                                (when-not include-deleted?
                                                  {:deleted false}))))))

(defn find-order-by-id [db order-id]
  (with-connection
    db (fn [conn-opts]
         (let [[order] (sql/find-by-keys conn-opts :user-order {:id order-id})
               items (find-all-order-items conn-opts order-id)]
           (when (seq order)
             (-> order
                 (db-str->keyword :user-order/amount-unit :user-order/status)
                 (assoc :user-order/items items)))))))

(defn insert-order! [db order]
  (sql/insert! db :user-order (keyword->db-str order :amount-unit :status)))

(defn update-order! [db order]
  (-> (sql/update! db :user-order (keyword->db-str order :amount-unit :status) (select-keys order [:id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-order! [db order-id]
  (-> (sql/update! db :user-order {:deleted true} {:id order-id})
      ::jdbc/update-count
      (pos?)))

(defn insert-order-item! [db item]
  (sql/insert! db :order-item (keyword->db-str item :status :amount-unit)))

(defn update-order-item! [db item]
  (-> item
      (keyword->db-str :status :amount-unit)
      (#(sql/update! db :order-item %
                     (select-keys % [:id])))
      ::jdbc/update-count
      (pos?)))

(defn delete-order-item! [db item]
  (-> (sql/delete! db :order-item item)
      ::jdbc/update-count
      (pos?)))

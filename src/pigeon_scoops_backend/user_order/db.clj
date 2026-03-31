(ns pigeon-scoops-backend.user-order.db
  (:require [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.utils :refer [apply-db-str->keyword
                                                 apply-keyword->db-str
                                                 keyword->db-str
                                                 with-connection]]
            [pigeon-scoops-backend.user-order.transforms :refer [infer-order-status]]))

(defn find-all-order-items [db order-id]
  (map #(apply-db-str->keyword %
                               :order-item/status :order-item/amount-unit)
       (sql/query db (-> (h/select :order-item/* :recipe/name)
                         (h/from :order-item)
                         (h/left-join :recipe [:= :order-item/recipe-id :recipe/id])
                         (h/where [:= :order-item/order-id order-id])
                         (hsql/format)))))

(defn find-all-orders
  [db user-id]
  (with-connection
    db (fn [db]
         (->> (sql/find-by-keys
               db
               :user-order
               (if user-id
                 {:user-order/user-id user-id}
                 :all))
              (mapv #(assoc % :user-order/status
                            (infer-order-status
                             (find-all-order-items db (:user-order/id %)))))
              (map #(apply-db-str->keyword % :user-order/amount-unit :user-order/status))))))

(defn find-order-by-id [db order-id]
  (with-connection
    db (fn [db]
         (let [[order] (sql/find-by-keys db :user-order {:id order-id})
               items (find-all-order-items db order-id)]
           (when (seq order)
             (-> order
                 (assoc :user-order/status (infer-order-status items))
                 (apply-db-str->keyword :user-order/amount-unit :user-order/status)
                 (assoc :user-order/items items)))))))

(defn find-order-item-by-id [db order-item-id]
  (-> (sql/find-by-keys db :order-item {:id order-item-id})
      (first)
      (apply-db-str->keyword :order-item/status :order-item/amount-unit)))

(defn insert-order! [db order]
  (sql/insert! db :user-order (apply-keyword->db-str order :user-order/amount-unit)))

(defn update-order! [db order]
  (-> (sql/update! db :user-order (apply-keyword->db-str order :user-order/amount-unit) (select-keys order [:user-order/id]))
      ::jdbc/update-count
      (pos?)))

(defn insert-order-item! [db item]
  (sql/insert! db :order-item (apply-keyword->db-str item :order-item/status :order-item/amount-unit)))

(defn update-order-item! [db item]
  (-> item
      (apply-keyword->db-str :order-item/status :order-item/amount-unit)
      (#(sql/update! db :order-item %
                     (select-keys % [:order-item/id])))
      ::jdbc/update-count
      (pos?)))

(defn delete-order-item! [db item]
  (-> (sql/delete! db :order-item item)
      ::jdbc/update-count
      (pos?)))

(defn accept-orders! [db & recipe-ids]
  (jdbc/with-transaction
    [tx db]
    (sql/query tx (-> (h/update :order-item)
                      (h/set {:order-item/status (keyword->db-str :status/in-progress)})
                      (h/where [:in :order-item/recipe-id recipe-ids]
                               [:= :order-item/status (keyword->db-str :status/submitted)])
                      (hsql/format)))
    (sql/query tx (-> (h/update :order-item)
                      (h/set {:order-item/status (keyword->db-str :status/canceled)})
                      (h/where [:in :order-item/recipe-id recipe-ids]
                               [:= :order-item/status (keyword->db-str :status/draft)])
                      (hsql/format)))))


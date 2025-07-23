(ns pigeon-scoops-backend.menu.db
  (:require [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.utils :refer [apply-db-str->keyword
                                                 apply-keyword->db-str
                                                 with-connection]])
  (:import (java.sql Timestamp)))

(defn find-menu-items [db menu-id & menu-ids]
  (with-connection
    db
    (fn [conn-opts]
      (let [menu-ids (conj menu-ids menu-id)
            menu-items (sql/query conn-opts
                                  (-> (h/select :*)
                                      (h/from :menu-item)
                                      (h/where [:in :menu-id menu-ids])
                                      (hsql/format)))
            menu-item-sizes (when (seq menu-items)
                              (->> (-> (h/select :*)
                                       (h/from :menu-item-size)
                                       (h/where [:in :menu-item-id (map :menu-item/id menu-items)])
                                       (hsql/format))
                                   (sql/query conn-opts)
                                   (map #(apply-db-str->keyword % :menu-item-size/amount-unit))
                                   (group-by :menu-item-size/menu-item-id)))]
        (->> menu-items
             (map #(assoc % :menu-item/sizes (get menu-item-sizes (:menu-item/id %))))
             (group-by :menu-item/menu-id))))))

(defn find-menu-item-by-id [db menu-item-id]
  (with-connection
    db
    (fn [conn-opts]
      (when-let [[item] (sql/find-by-keys conn-opts :menu-item {:id menu-item-id})]
        (assoc item :menu-item/sizes (sql/find-by-keys conn-opts :menu-item-size {:menu-item-id menu-item-id}))))))

(defn find-all-menus
  ([db]
   (find-all-menus db false))
  ([db include-inactive?]
   (with-connection
     db
     (fn [conn-opts]
       (let [menus (sql/find-by-keys
                     conn-opts
                     :menu
                     (if include-inactive? :all {:active true}))
             menu-items (when (seq menus) (apply (partial find-menu-items conn-opts) (map :menu/id menus)))]
         (->> menus
              (map #(assoc % :menu/items (get menu-items (:menu/id %))))
              (map #(apply-db-str->keyword % :menu/duration-type))))))))


(defn insert-menu! [db menu]
  (sql/insert! db :menu
               (-> menu
                   (apply-keyword->db-str :duration-type)
                   (update :end-time #(when % (Timestamp/from (.toInstant %)))))))

(defn find-menu-by-id [db menu-id]
  (with-connection
    db
    (fn [conn-opts]
      (when-let [[menu] (sql/find-by-keys conn-opts :menu {:menu/id menu-id})]
        (-> menu
            (assoc :menu/items (get (find-menu-items conn-opts menu-id) menu-id))
            (apply-db-str->keyword :menu/duration-type))))))

(defn update-menu! [db menu]
  (-> (sql/update! db :menu
                   (-> menu
                       (apply-keyword->db-str :duration-type)
                       (update :end-time #(when % (Timestamp/from (.toInstant %)))))
                   (select-keys menu [:id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-menu! [db menu]
  (-> (sql/delete! db :menu menu)
      ::jdbc/update-count
      (pos?)))

(defn insert-menu-item! [db menu-item]
  (sql/insert! db :menu-item menu-item))

(defn update-menu-item! [db menu-item]
  (-> (sql/update! db :menu-item menu-item (select-keys menu-item [:id :menu-id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-menu-item! [db menu-item]
  (-> (sql/delete! db :menu-item menu-item)
      ::jdbc/update-count
      (pos?)))

(defn insert-menu-item-size! [db menu-item-size]
  (sql/insert! db :menu-item-size (apply-keyword->db-str menu-item-size :amount-unit)))

(defn update-menu-item-size! [db menu-item-size]
  (-> (sql/update! db :menu-item-size
                   (apply-keyword->db-str menu-item-size :amount-unit)
                   (select-keys menu-item-size [:id :menu-id :menu-item-id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-menu-item-size! [db menu-item-size]
  (-> (sql/delete! db :menu-item-size menu-item-size)
      ::jdbc/update-count
      (pos?)))

(defn find-active-menu-items [db]
  (->> (-> (h/select :menu-item/*)
           (h/from :menu-item)
           (h/join :menu [:= :menu/id :menu-item/menu-id])
           (h/where [:= :menu/active true])
           (hsql/format))
       (sql/query db)))

(defn find-menu-item-sizes [db & menu-item-ids]
  (->> (-> (h/select :menu-item-size/*)
           (h/from :menu-item-size)
           (h/where [:in :menu-item-id menu-item-ids])
           (hsql/format))
       (sql/query db)
       (map #(apply-db-str->keyword % :menu-item-size/amount-unit))))

(comment
  (do
    (require '[integrant.repl.state :as state])
    (let [items (find-active-menu-items (:db/postgres state/system))]
      (apply (partial find-menu-item-sizes db)
             (map :menu-item/id items)))))


(ns pigeon-scoops-backend.menu.db
  (:require [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.utils :refer [db-str->keyword
                                                 keyword->db-str
                                                 with-connection]])
  (:import (java.sql Timestamp)
           (java.time ZonedDateTime)))

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
                              (group-by
                                :menu-item-size/menu-item-id
                                (sql/query conn-opts
                                           (-> (h/select :*)
                                               (h/from :menu-item-size)
                                               (h/where [:in :menu-id (map :menu-item/id menu-items)])
                                               (hsql/format)))))]
        (->> menu-items
             (map #(assoc % :menu-item/sizes (get menu-item-sizes (:menu-item/id %))))
             (group-by :menu-item/id))))))

(defn find-menu-item-by-id [db menu-item-id]
  (with-connection
    db
    (fn [conn-opts]
      (when-let [item (sql/find-by-keys conn-opts :menu-item {:id menu-item-id})]
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
              (map #(update % :menu/duration-type db-str->keyword))))))))


(defn insert-menu! [db menu]
  (sql/insert! db :menu
               (-> menu
                   (keyword->db-str :duration-type)
                   (update :end-time #(when % (Timestamp/from (.toInstant %)))))))

(defn find-menu-by-id [db menu-id]
  (with-connection
    db
    (fn [conn-opts]
      (when-let [menu (sql/find-by-keys conn-opts :menu {:menu/id menu-id})]
        (-> menu
            (assoc :menu/items (vals (find-menu-items conn-opts menu-id)))
            (update :menu/duration-type db-str->keyword))))))

(defn update-menu! [db menu]
  (sql/update! db :menu
               (keyword->db-str menu :duration-type)
               (select-keys menu [:id])))

(defn delete-menu! [db menu]
  (-> (sql/delete! db :menu menu)
      ::jdbc/update-count
      (pos?)))

(defn insert-menu-item! [db menu-item]
  (sql/insert! db :menu-item menu-item))

(defn update-menu-item! [db menu-item]
  (sql/update! db :menu-item menu-item (select-keys menu-item [:id])))

(defn delete-menu-item! [db menu-item]
  (-> (sql/delete! db :menu-item menu-item)
      ::jdbc/update-count
      (pos?)))

(defn insert-menu-item-size! [db menu-item-size]
  (sql/insert! db :menu-item-size (keyword->db-str menu-item-size :amount-unit)))

(defn update-menu-item-size! [db menu-item-size]
  (sql/update! db :menu-item-size
               (keyword->db-str menu-item-size :amount-unit)
               (select-keys menu-item-size [:id])))

(defn delete-menu-item-size! [db menu-item-size]
  (-> (sql/delete! db :menu-item-size menu-item-size)
      ::jdbc/update-count
      (pos?)))

(comment
  (do
    (require '[integrant.repl.state :as state])
    (import '[java.util UUID])
    (import '[java.time ZonedDateTime]))
  (insert-menu! (:db/postgres state/system) {:id            (UUID/randomUUID)
                                             :name          "foobar menu"
                                             :repeats       true
                                             :active        false
                                             :duration      69
                                             :duration-type :duration/day
                                             :end-time      (ZonedDateTime/now)})
  (find-all-menus (:db/postgres state/system) true))
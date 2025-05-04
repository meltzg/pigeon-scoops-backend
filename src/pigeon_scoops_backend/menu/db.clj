(ns pigeon-scoops-backend.menu.db
  (:require [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [integrant.repl.state :as state]
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
                              (->> (-> (h/select :*)
                                       (h/from :menu-item-size)
                                       (h/where [:in :menu-item-id (map :menu-item/id menu-items)])
                                       (hsql/format))
                                   (sql/query conn-opts)
                                   (map #(db-str->keyword % :menu-item-size/amount-unit))
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
              (map #(db-str->keyword % :menu/duration-type))))))))


(defn insert-menu! [db menu]
  (sql/insert! db :menu
               (-> menu
                   (keyword->db-str :duration-type)
                   (update :end-time #(when % (Timestamp/from (.toInstant %)))))))

(defn find-menu-by-id [db menu-id]
  (with-connection
    db
    (fn [conn-opts]
      (when-let [[menu] (sql/find-by-keys conn-opts :menu {:menu/id menu-id})]
        (-> menu
            (assoc :menu/items (get (find-menu-items conn-opts menu-id) menu-id))
            (db-str->keyword :menu/duration-type))))))

(defn update-menu! [db menu]
  (-> (sql/update! db :menu
                   (-> menu
                       (keyword->db-str :duration-type)
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
  (sql/insert! db :menu-item-size (keyword->db-str menu-item-size :amount-unit)))

(defn update-menu-item-size! [db menu-item-size]
  (-> (sql/update! db :menu-item-size
                   (keyword->db-str menu-item-size :amount-unit)
                   (select-keys menu-item-size [:id :menu-id :menu-item-id]))
      ::jdbc/update-count
      (pos?)))

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
  (insert-menu-item! (:db/postgres state/system) {:id        (UUID/randomUUID)
                                                  :recipe-id #uuid"3733eda5-3c1c-4e48-90d5-854cd1c79d00"
                                                  :menu-id   #uuid"526dc6c6-6bd3-4ac4-9fa1-307e729ab941"})
  (insert-menu-item-size! (:db/postgres state/system) {:id           (UUID/randomUUID)
                                                       :menu-item-id #uuid"84b1e12b-78fa-4a51-bddd-c885ad7b146c"
                                                       :menu-id      #uuid"526dc6c6-6bd3-4ac4-9fa1-307e729ab941"
                                                       :amount       4
                                                       :amount-unit  :volume/gal})
  (find-menu-by-id (:db/postgres state/system) #uuid"526dc6c6-6bd3-4ac4-9fa1-307e729ab941")
  (find-all-menus (:db/postgres state/system) true))


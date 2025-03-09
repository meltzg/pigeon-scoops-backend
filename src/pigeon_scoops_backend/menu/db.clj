(ns pigeon-scoops-backend.menu.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.utils :refer [db-str->keyword
                                                 keyword->db-str]])
  (:import (java.sql Timestamp)
           (java.util UUID)))

(defn find-all-menus [db])

(defn insert-menu! [db menu]
  (sql/insert! db :menu
               (-> menu
                   (keyword->db-str :duration-type)
                   (update :end-time #(when % (Timestamp/from %))))))

(defn find-menu-by-id [db menu-id])

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

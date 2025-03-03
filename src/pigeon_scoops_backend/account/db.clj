(ns pigeon-scoops-backend.account.db
  (:require [next.jdbc.sql :as sql]))

(defn create-account! [db account]
  (sql/insert! db :account account))

(defn delete-account! [db account]
  (sql/delete! db :account account))

(defn find-account-by-id [db account-id]
  (first (sql/find-by-keys db :account {:id account-id})))

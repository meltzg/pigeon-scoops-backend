(ns pigeon-scoops-backend.order.handlers
  (:require [pigeon-scoops-backend.order.db :as order-db]
            [pigeon-scoops-backend.responses :as responses]
            [ring.util.response :as rr])
  (:import (java.util UUID)))

(defn list-all-orders [db]
  (fn [request]
    (let [uid (-> request :claims :sub)]
      (rr/response (vec (order-db/find-all-orders db uid))))))

(defn create-order! [db]
  (fn [request]
    (let [order-id (UUID/randomUUID)
          uid (-> request :claims :sub)
          order (-> request :parameters :body)]
      (order-db/insert-order! db (assoc order
                                   :id order-id
                                   :user-id uid
                                   :status :status/draft))
      (rr/created (str responses/base-url "/orders/" order-id)
                  {:id order-id}))))

(defn retrieve-order [db]
  (fn [request]
    (let [order-id (-> request :parameters :path :order-id)
          order (order-db/find-order-by-id db order-id)]
      (if order
        (rr/response (update order :order/units vec))
        (rr/not-found {:type    "order-not-found"
                       :message "order not found"
                       :data    (str "order-id " order-id)})))))

(defn update-order! [db]
  (fn [request]
    (let [order-id (-> request :parameters :path :order-id)
          order (-> request :parameters :body)
          successful? (order-db/update-order! db (assoc order :id order-id))]
      (if successful?
        (rr/status 204)
        (rr/not-found {:type    "order-not-found"
                       :message "order not found"
                       :data    (str "order-id " order-id)})))))

(defn delete-order! [db]
  (fn [request]
    (let [order-id (-> request :parameters :path :order-id)
          successful? (order-db/delete-order! db order-id)]
      (if successful?
        (rr/status 204)
        (rr/not-found {:type    "order-not-found"
                       :message "order not found"
                       :data    (str "order-id " order-id)})))))

(defn create-order-item! [db]
  (fn [request]
    (let [order-id (-> request :parameters :path :order-id)
          order-item (-> request :parameters :body)
          order-item-id (UUID/randomUUID)]
      (order-db/insert-order-item! db (assoc order-item
                                        :order-id order-id
                                        :id order-item-id
                                        :status :status/draft))
      (rr/created (str responses/base-url "/orders/" order-id)
                  {:id order-item-id}))))

(defn update-order-item! [db]
  (fn [request]
    (let [order-id (-> request :parameters :path :order-id)
          order-item (-> request :parameters :body)
          successful? (order-db/update-order-item! db (assoc order-item :order-id order-id))]
      (if successful?
        (rr/status 204)
        (rr/bad-request (select-keys order-item [:id]))))))

(defn delete-order-item! [db]
  (fn [request]
    (let [order-id (-> request :parameters :path :order-id)
          order-item-id (-> request :parameters :body :id)
          successful? (order-db/delete-order-item! db {:id order-item-id :order-id order-id})]
      (if successful?
        (rr/status 204)
        (rr/bad-request (-> request :parameters :body))))))

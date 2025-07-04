(ns pigeon-scoops-backend.user-order.handlers
  (:require [pigeon-scoops-backend.grocery.db :refer [find-grocery-by-id]]
            [pigeon-scoops-backend.grocery.transforms :refer [grocery-for-amount]]
            [pigeon-scoops-backend.menu.db :as menu-db]
            [pigeon-scoops-backend.recipe.db :refer [ingredient-bom]]
            [pigeon-scoops-backend.recipe.transforms :refer [combine-ingredients]]
            [pigeon-scoops-backend.responses :as responses]
            [pigeon-scoops-backend.units.common :as units]
            [pigeon-scoops-backend.user-order.db :as order-db]
            [pigeon-scoops-backend.utils :refer [with-connection]]
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
        (rr/response (update order :user-order/items vec))
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
    (with-connection
      db
      (fn [conn-opts]
        (let [order-id (-> request :parameters :path :order-id)
              {:keys [recipe-id amount amount-unit] :as order-item} (-> request :parameters :body)
              order-item-id (UUID/randomUUID)
              active-items (get
                             (group-by
                               :menu-item/recipe-id
                               (menu-db/find-active-menu-items conn-opts))
                             recipe-id)
              active-item-sizes (apply (partial menu-db/find-menu-item-sizes conn-opts)
                                       (map :menu-item/id (vals active-items)))]
          (cond
            (nil? (get active-items recipe-id))
            (rr/bad-request {:type    "recipe-not-in-active-menu"
                             :message "recipe is not in an active menu"
                             :data    (str "recipe-id " recipe-id)})
            (not-any? #(zero?
                  (first
                    (units/reduce-amounts mod amount amount-unit
                                          (:menu-item-size/amount %)
                                          (:menu-item-size/amount-unit))))
                  active-item-sizes)
            (rr/bad-request {:type "no-valid-size-order-amount"
                             :message "order amount cannot be made from any active item size"
                             :data active-item-sizes})
            :else
            (do
              (order-db/insert-order-item! conn-opts (assoc order-item
                                                       :order-id order-id
                                                       :id order-item-id
                                                       :status :status/draft))
              (rr/created (str responses/base-url "/orders/" order-id)
                          {:id order-item-id}))))))))


(defn update-order-item! [db]
  (fn [request]
    (let [order-id (-> request :parameters :path :order-id)
          {:keys [recipe-id] :as order-item} (-> request :parameters :body)])))
      ;(if (recipe-in-active-menu? db recipe-id)
      ;  (let [successful? (order-db/update-order-item! db (assoc order-item :order-id order-id))]
      ;    (if successful?
      ;      (rr/status 204)
      ;      (rr/bad-request (select-keys order-item [:id]))))
      ;  (rr/bad-request {:type    "recipe-not-in-active-menu"
      ;                   :message "recipe is not in an active menu"
      ;                   :data    (str "recipe-id " recipe-id)})))))

(defn delete-order-item! [db]
  (fn [request]
    (let [order-id (-> request :parameters :path :order-id)
          order-item-id (-> request :parameters :body :id)
          successful? (order-db/delete-order-item! db {:id order-item-id :order-id order-id})]
      (if successful?
        (rr/status 204)
        (rr/bad-request (-> request :parameters :body))))))

(defn retrieve-order-bom [db]
  (fn [request]
    (with-connection
      db
      (fn [conn-opts]
        (let [order-id (-> request :parameters :path :order-id)
              {:user-order/keys [items]} (order-db/find-order-by-id db order-id)
              grocery-bom (->> items
                               (mapcat #(ingredient-bom conn-opts {:recipe/id          (:order-item/recipe-id %)
                                                                   :recipe/amount      (:order-item/amount %)
                                                                   :recipe/amount-unit (:order-item/amount-unit %)}))
                               (combine-ingredients)
                               (map #(update (grocery-for-amount
                                               (find-grocery-by-id conn-opts (:ingredient/ingredient-grocery-id %))
                                               (:ingredient/amount %)
                                               (:ingredient/amount-unit %))
                                             :grocery/units
                                             vec)))]
          (rr/response (vec grocery-bom)))))))

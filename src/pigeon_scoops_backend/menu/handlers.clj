(ns pigeon-scoops-backend.menu.handlers
  (:require [pigeon-scoops-backend.menu.db :as menu-db]
            [pigeon-scoops-backend.responses :as responses]
            [pigeon-scoops-backend.utils :refer [end-time]]
            [ring.util.response :as rr])
  (:import (java.util UUID)))

(defn list-all-menus [db]
  (fn [_]
    (rr/response (vec (menu-db/find-all-menus db)))))

(defn create-menu! [db]
  (fn [request]
    (let [menu-id (UUID/randomUUID)
          menu (-> request :parameters :body)]
      (menu-db/insert-menu! db (cond-> (assoc menu :id menu-id)
                                       (:active menu) (assoc :end-time (-> menu
                                                                           (select-keys [:duration :duration-type])
                                                                           (vals)
                                                                           ((partial apply end-time))))
                                       (not (:active menu)) (assoc :end-time nil)))
      (rr/created (str responses/base-url "/menus/" menu-id)
                  {:id menu-id}))))

(defn retrieve-menu [db]
  (fn [request]
    (let [menu-id (-> request :parameters :path :menu-id)
          menu (menu-db/find-menu-by-id db menu-id)]
      (if menu
        (rr/response (update menu
                             :menu/items
                             (partial mapv #(update % :menu-item/sizes vec))))
        (rr/not-found {:type    "menu-not-found"
                       :message "menu not found"
                       :data    (str "menu-id " menu-id)})))))

(defn update-menu! [db]
  (fn [request]
    (let [menu-id (-> request :parameters :path :menu-id)
          menu (-> request :parameters :body)
          current-menu (menu-db/find-menu-by-id db menu-id)
          successful? (menu-db/update-menu! db (cond-> (assoc menu :id menu-id
                                                                   :end-time (:menu/end-time current-menu))
                                                       (and (:active menu) (not (:menu/active current-menu)))
                                                       (assoc :end-time (-> menu
                                                                            (select-keys [:duration :duration-type])
                                                                            (vals)
                                                                            ((partial apply end-time))))
                                                       (and (not (:active menu)) (:menu/active current-menu))
                                                       (assoc :end-time nil)))]
      (if successful?
        (rr/status 204)
        (rr/not-found {:type    "menu-not-found"
                       :message "menu not found"
                       :data    (str "menu-id " menu-id)})))))

(defn delete-menu! [db]
  (fn [request]
    (let [menu-id (-> request :parameters :path :menu-id)
          successful? (menu-db/delete-menu! db menu-id)]
      (if successful?
        (rr/status 204)
        (rr/not-found {:type    "menu-not-found"
                       :message "menu not found"
                       :data    (str "menu-id " menu-id)})))))

(defn create-menu-item! [db]
  (fn [request]
    (let [menu-id (-> request :parameters :path :menu-id)
          menu-item (-> request :parameters :body)
          menu-item-id (UUID/randomUUID)]
      (menu-db/insert-menu-item! db (assoc menu-item :menu-id menu-id
                                                     :id menu-item-id))
      (rr/created (str responses/base-url "/menus/" menu-id)
                  {:id menu-item-id}))))

(defn update-menu-item! [db]
  (fn [request]
    (let [menu-id (-> request :parameters :path :menu-id)
          menu-item (-> request :parameters :body)
          successful? (menu-db/update-menu-item! db (assoc menu-item :menu-id menu-id))]
      (if successful?
        (rr/status 204)
        (rr/bad-request (select-keys menu-item [:id]))))))

(defn delete-menu-item! [db]
  (fn [request]
    (let [menu-id (-> request :parameters :path :menu-id)
          menu-item-id (-> request :parameters :body :id)
          successful? (menu-db/delete-menu-item! db {:id menu-item-id :menu-id menu-id})]
      (if successful?
        (rr/status 204)
        (rr/bad-request (-> request :parameters :body))))))

(defn create-menu-item-size! [db]
  (fn [request]
    (let [menu-id (-> request :parameters :path :menu-id)
          {:keys [menu-item-id] :as menu-item-size} (-> request :parameters :body)
          menu-item (menu-db/find-menu-item-by-id db menu-item-id)
          menu-item-size-id (UUID/randomUUID)]
      (if (= menu-id (:menu-item/menu-id menu-item))
        (do (menu-db/insert-menu-item-size! db (assoc menu-item-size :id menu-item-size-id :menu-id menu-id))
            (rr/created (str responses/base-url "/menus/" menu-id)
                        {:id menu-item-size-id}))
        (rr/bad-request {:type    "menu-mismatch"
                         :message (str "menu-item does not belong to provided menu")
                         :data    (str "menu-id " menu-id)})))))

(defn update-menu-item-size! [db]
  (fn [request]
    (let [menu-id (-> request :parameters :path :menu-id)
          {:keys [menu-item-id] :as menu-item-size} (-> request :parameters :body)
          menu-item (menu-db/find-menu-item-by-id db menu-item-id)]
      (cond
        (not= menu-id (:menu-item/menu-id menu-item))
        (rr/bad-request {:type    "menu-mismatch"
                         :message (str "menu-item does not belong to provided menu")
                         :data    (str "menu-id " menu-id)})
        (menu-db/update-menu-item-size! db (assoc menu-item-size :menu-id menu-id))
        (rr/status 204)
        :else
        (rr/bad-request (select-keys menu-item-size [:id]))))))


(defn delete-menu-item-size! [db]
  (fn [request]
    (let [menu-id (-> request :parameters :path :menu-id)
          menu-item-size-id (-> request :parameters :body :id)
          successful? (menu-db/delete-menu-item-size! db {:id menu-item-size-id :menu-id menu-id})]
      (if successful?
        (rr/status 204)
        (rr/bad-request (-> request :parameters :body))))))

(ns pigeon-scoops-backend.recipe.handlers
  (:require [pigeon-scoops-backend.recipe.db :as recipe-db]
            [pigeon-scoops-backend.responses :as responses]
            [ring.util.response :as rr])
  (:import (java.util UUID)))

(defn list-all-recipes [db]
  (fn [request]
    (let [uid (-> request :claims :sub)
          recipes (recipe-db/find-all-recipes db uid)]
      (rr/response (update-vals recipes vec)))))

(defn create-recipe! [db]
  (fn [request]
    (let [recipe-id (UUID/randomUUID)
          uid (-> request :claims :sub)
          recipe (-> request :parameters :body)]
      (recipe-db/insert-recipe! db (assoc recipe :id recipe-id
                                                 :user-id uid))
      (rr/created (str responses/base-url "/recipes/" recipe-id)
                  {:id recipe-id}))))

(defn retrieve-recipe [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          recipe (recipe-db/find-recipe-by-id db recipe-id)]
      (if recipe
        (rr/response (update recipe :recipe/ingredients vec))
        (rr/not-found {:type    "recipe-not-found"
                       :message "Recipe not found"
                       :data    (str "recipe-id " recipe-id)})))))

(defn update-recipe! [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          recipe (-> request :parameters :body)
          successful? (recipe-db/update-recipe! db (assoc recipe :id recipe-id))]
      (if successful?
        (rr/status 204)
        (rr/not-found {:type    "recipe-not-found"
                       :message "Recipe not found"
                       :data    (str "recipe-id " recipe-id)})))))

(defn delete-recipe! [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          successful? (recipe-db/delete-recipe! db recipe-id)]
      (if successful?
        (rr/status 204)
        (rr/not-found {:type    "recipe-not-found"
                       :message "Recipe not found"
                       :data    (str "recipe-id " recipe-id)})))))

(defn favorite-recipe! [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          uid (-> request :claims :sub)]
      (recipe-db/favorite-recipe! db {:recipe-id recipe-id :user-id uid})
      (rr/status 204))))

(defn unfavorite-recipe! [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          uid (-> request :claims :sub)
          successful? (recipe-db/unfavorite-recipe! db {:recipe-id recipe-id :user-id uid})]
      (if successful?
        (rr/status 204)
        (rr/not-found {:type    "recipe-not-found"
                       :message "Recipe not found"
                       :data    (str "recipe-id " recipe-id)})))))

(defn create-ingredient! [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          ingredient (-> request :parameters :body)
          ingredient-id (UUID/randomUUID)]
      (recipe-db/insert-ingredient! db (assoc ingredient :recipe-id recipe-id
                                                         :id ingredient-id))
      (rr/created (str responses/base-url "/recipes/" recipe-id)
                  {:id ingredient-id}))))

(defn update-ingredient! [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          ingredient (-> request :parameters :body)
          successful? (recipe-db/update-ingredient! db (assoc ingredient :recipe-id recipe-id))]
      (if successful?
        (rr/status 204)
        (rr/bad-request (select-keys ingredient [:id]))))))

(defn delete-ingredient! [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          ingredient (-> request :parameters :body)
          successful? (recipe-db/delete-ingredient! db (assoc (select-keys ingredient [:id])
                                                         :recipe-id recipe-id))]
      (if successful?
        (rr/status 204)
        (rr/bad-request (select-keys ingredient [:id]))))))
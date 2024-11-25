(ns pigeon-scoops-backend.recipe.handlers
  (:require [pigeon-scoops-backend.recipe.db :as recipe-db]
            [pigeon-scoops-backend.responses :as responses]
            [ring.util.response :as rr])
  (:import (java.util UUID)))

(defn list-all-recipes [db]
  (fn [request]
    (let [uid (-> request :claims :sub)
          recipes (recipe-db/find-all-recipes db uid)]
      (println "uid" (:claims request))
      (rr/response recipes))))

(defn create-recipe! [db]
  (fn [request]
    (let [recipe-id (str (UUID/randomUUID))
          uid (-> request :claims :sub)
          recipe (-> request :parameters :body)]
      (recipe-db/insert-recipe! db (assoc recipe :recipe-id recipe-id
                                                 :uid uid))
      (rr/created (str responses/base-url "/recipes/" recipe-id)
                  {:recipe-id recipe-id}))))

(defn retrieve-recipe [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          recipe (recipe-db/find-recipe-by-id db recipe-id)]
      (if recipe
        (rr/response recipe)
        (rr/not-found {:type    "recipe-not-found"
                       :message "Recipe not found"
                       :data    (str "recipe-id " recipe-id)})))))

(defn update-recipe! [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          recipe (-> request :parameters :body)
          update-successful? (recipe-db/update-recipe! db (assoc recipe :recipe-id recipe-id))]
      (if update-successful?
        (rr/status 204)
        (rr/not-found {:type    "recipe-not-found"
                       :message "Recipe not found"
                       :data    (str "recipe-id " recipe-id)})))))

(defn delete-recipe! [db]
  (fn [request]
    (let [recipe-id (-> request :parameters :path :recipe-id)
          delete-successful? (recipe-db/delete-recipe! db recipe-id)]
      (if delete-successful?
        (rr/status 204)
        (rr/not-found {:type    "recipe-not-found"
                       :message "Recipe not found"
                       :data    (str "recipe-id " recipe-id)})))))
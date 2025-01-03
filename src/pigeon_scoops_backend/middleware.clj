(ns pigeon-scoops-backend.middleware
  (:require [pigeon-scoops-backend.recipe.db :as recipe-db]
            [ring.middleware.jwt :as jwt]
            [ring.util.response :as rr]))

(def wrap-auth0
  {:name        ::auth0
   :description "Middleware for auth0 authentication and authorization"
   :wrap        (fn [handler]
                  (jwt/wrap-jwt
                    handler
                    {:issuers {"https://pigeon-scoops.us.auth0.com/"
                               {:alg          :RS256
                                :jwk-endpoint "https://pigeon-scoops.us.auth0.com/.well-known/jwks.json"}}}))})

(def wrap-recipe-owner
  {:name        ::recipe-owner
   :description "Middleware to check if a request user is a recipe owner"
   :wrap        (fn [handler db]
                  (fn [request]
                    (let [uid (-> request :claims :sub)
                          recipe-id (-> request :parameters :path :recipe-id)
                          recipe (recipe-db/find-recipe-by-id db recipe-id)]
                      (if (= (:recipe/user-id recipe) uid)
                        (handler request)
                        (-> (rr/response {:message "Operation requires recipe ownership"
                                          :data    (str "recipe-id " recipe-id)
                                          :type    :authorization-required})
                            (rr/status 401))))))})

(defn wrap-with-roles [& required-roles]
  {:name        ::manage-recipes
   :description (str "Middleware to check if a user has any of" required-roles " roles")
   :wrap        (fn [handler]
                  (fn [request]
                    (let [roles (get-in request [:claims "https://api.pigeon-scoops.com/roles"])]
                      (if (some (set required-roles) roles)
                        (handler request)
                        (-> (rr/response {:message (str "Operation requires any of " required-roles " roles")
                                          :data    (:uri request)
                                          :type    :authorization-required})
                            (rr/status 401))))))})

(def wrap-manage-recipes
  (wrap-with-roles "manage-recipes"))

(def wrap-manage-roles
  (wrap-with-roles "manage-roles"))

(def wrap-manage-groceries
  (wrap-with-roles "manage-groceries"))

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
                    {:alg          :RS256
                     :jwk-endpoint "https://pigeon-scoops.us.auth0.com/.well-known/jwks.json"}))})

(def wrap-recipe-owner
  {:name        ::recipe-owner
   :description "Middleware to check if a request user is a recipe owner"
   :wrap        (fn [handler db]
                  (fn [request]
                    (let [uid (-> request :claims :sub)
                          recipe-id (-> request :parameters :path :recipe-id)
                          recipe (recipe-db/find-recipe-by-id db recipe-id)]
                      (if (= (:recipe/uid recipe) uid)
                        (handler request)
                        (-> (rr/response {:message "Operation requires recipe ownership"
                                          :data    (str "recipe-id " recipe-id)
                                          :type    :authorization-required})
                            (rr/status 401))))))})

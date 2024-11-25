(ns pigeon-scoops-backend.middleware
  (:require [ring.middleware.jwt :as jwt]))

(def wrap-auth0 {:name        ::auth0
                 :description "Middleware for auth0 authentication and authorization"
                 :wrap        (fn [handler]
                                (jwt/wrap-jwt
                                  handler
                                  {:alg          :RS256
                                   :jwk-endpoint "https://pigeon-scoops.us.auth0.com/.well-known/jwks.json"}))})

(ns pigeon-scoops-backend.middleware
  (:require [clojure.string :as str]
            [ring.middleware.cors :refer [wrap-cors]]
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

(def wrap-cors-configured
  {:name        ::cors-configured
   :description "Middleware for CORS preconfigured for this app"
   :wrap        (fn [handler]
                  (wrap-cors
                    handler
                    :access-control-allow-origin [#"http://localhost:3000"
                                                  #"https://pigeon-scoops.com"]
                    :access-control-allow-methods [:get :post :put :delete]))})


(defn wrap-owner
  ([id-key table find-by-id]
   (wrap-owner id-key nil table find-by-id))
  ([id-key public-key table find-by-id]
   {:name        (keyword (str *ns*) (str (name table)
                                          (when public-key
                                            (str "-" (name public-key)))))
    :description (str "Middleware to check if a request user is an" table " owner")
    :wrap        (fn [handler db]
                   (fn [request]
                     (let [uid (-> request :claims :sub)
                           entity-id (-> request :parameters :path id-key)
                           entity (find-by-id db entity-id)]
                       (if (or (= ((keyword (name table) "user-id") entity) uid)
                               (get entity public-key))
                         (handler request)
                         (-> (rr/response {:message (str "Operation requires " (name table) " ownership")
                                           :data    (str "entity-id " entity-id)
                                           :type    :authorization-required})
                             (rr/status 401))))))}))

(defn wrap-with-permission [permission]
  {:name        (keyword (str *ns*) (str permission))
   :description (str "Middleware to check if a user has the " permission " permission")
   :wrap        (fn [handler]
                  (fn [request]
                    (let [permissions (map #(keyword (str/replace % ":" "/"))
                                           (get-in request [:claims "https://api.pigeon-scoops.com/perms"]))]
                      (if ((set permissions) permission)
                        (handler request)
                        (-> (rr/response {:message (str "Operation requires " permission " permission")
                                          :data    (:uri request)
                                          :type    :authorization-required})
                            (rr/status 401))))))})

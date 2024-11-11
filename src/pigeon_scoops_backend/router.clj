(ns pigeon-scoops-backend.router
  (:require [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.coercion.spec :as coercion-spec]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [pigeon-scoops-backend.recipe.routes :as recipe]))

(def router-config
  {:data {:coercion coercion-spec/coercion
          :muuntaja   m/instance
          :middleware [swagger/swagger-feature
                       muuntaja/format-middleware
                       exception/exception-middleware
                       coercion/coerce-request-middleware]}})

(def swagger-docs
  ["/swagger.json"
   {:get
    {:no-doc true
     :swagger {:basePath "/"
               :info {:title "Pigeon Scoops Backend Reference"
                      :description "The Pigeon Scoops Backend API is organized around REST. Returns JSON, Transit (msgpack, json), or EDN  encoded responses."
                      :version "1.0.0"}}
     :handler (swagger/create-swagger-handler)}}])

(defn routes [env]
  (ring/ring-handler
    (ring/router
      [swagger-docs
       ["/v1"
        (recipe/routes env)]]
      router-config)
    (ring/routes
      (swagger-ui/create-swagger-ui-handler {:path "/"}))))
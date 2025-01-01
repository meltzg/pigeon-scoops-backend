(ns pigeon-scoops-backend.router
  (:require [muuntaja.core :as m]
            [pigeon-scoops-backend.account.routes :as account]
            [pigeon-scoops-backend.recipe.routes :as recipe]
            [reitit.coercion.spec :as coercion-spec]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.dev :as dev]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.spec :as rs]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]))

(def router-config
  {:validate  rs/validate
   ;:reitit.middleware/transform dev/print-request-diffs
   :exception pretty/exception
   :data      {:coercion   coercion-spec/coercion
               :muuntaja   m/instance
               :middleware [swagger/swagger-feature
                            muuntaja/format-middleware
                            ;exception/exception-middleware
                            coercion/coerce-request-middleware
                            coercion/coerce-response-middleware]}})

(def swagger-docs
  ["/swagger.json"
   {:get
    {:no-doc  true
     :swagger {:basePath            "/"
               :info                {:title       "Pigeon Scoops Backend Reference"
                                     :description "The Pigeon Scoops Backend API is organized around REST. Returns JSON, Transit (msgpack, json), or EDN  encoded responses."
                                     :version     "1.0.0"}
               :securityDefinitions {:BearerAuth
                                     {:type        "apiKey"
                                      :name        "Authorization"
                                      :in          "header"
                                      :description "Token must be prepended with \"Bearer \""}}}
     :handler (swagger/create-swagger-handler)}}])

(defn routes [env]
  (ring/ring-handler
    (ring/router
      [swagger-docs
       ["/v1"
        {:swagger {:security [{:BearerAuth []}]}}
        (recipe/routes env)
        (account/routes env)]]
      router-config)
    (ring/routes
      (swagger-ui/create-swagger-ui-handler {:path "/"}))))
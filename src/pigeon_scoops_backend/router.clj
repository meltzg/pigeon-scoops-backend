(ns pigeon-scoops-backend.router
  (:require [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [pigeon-scoops-backend.recipe.routes :as recipe]))

(def router-config
  {:data {:muuntaja   m/instance
          :middleware [swagger/swagger-feature
                       muuntaja/format-middleware]}})

(def swagger-docs
  ["/swagger.json"
   {:get
    {:no-doc true
     :swagger {:basePath "/"
               :info {:title "Pigeon Scoops Backend Reference"
                      :description "recipe manager"
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
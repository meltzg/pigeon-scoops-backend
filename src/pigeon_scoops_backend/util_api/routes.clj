(ns pigeon-scoops-backend.util-api.routes
  (:require [pigeon-scoops-backend.middleware :as mw]
            [pigeon-scoops-backend.util-api.handlers :as util-api]
            [pigeon-scoops-backend.util-api.responses :as responses]))

(defn routes []
  ["" {:openapi    {:tags ["utilities"]}
       :middleware [[mw/wrap-auth0]]}
   ["/constants" {:get {:handler   (util-api/get-constants)
                        :responses {200 {:body responses/constants}}
                        :summary   "retrieve application constants"}}]])

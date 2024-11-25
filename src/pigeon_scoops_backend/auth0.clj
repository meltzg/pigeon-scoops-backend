(ns pigeon-scoops-backend.auth0
  (:require [clj-http.client :as http]
            [muuntaja.core :as m]))

(defn get-test-token []
  (->> {:content-type  :json
        :cookie-policy :standard
        :body          (m/encode "application/json"
                                 {:client_id  "AoU9LnGWQlCbSUvjgXdHf4NZPJh0VHYD"
                                  :audience   "https://pigeon-scoops.us.auth0.com/api/v2/"
                                  :grant_type "password"
                                  :username   "testing@pigeon-scoops.com"
                                  :password   "Testing123User"
                                  :scope      "openid profile email"})}
       (http/post "https://pigeon-scoops.us.auth0.com/oauth/token")
       (m/decode-response-body)
       :access_token))

(comment
  (get-test-token)
  )

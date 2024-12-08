(ns pigeon-scoops-backend.auth0
  (:require [clj-http.client :as http]
            [muuntaja.core :as m]))

(defn get-test-token [{:keys [test-client-id test-user test-password]}]
  (->> {:content-type  :json
        :cookie-policy :standard
        :body          (m/encode "application/json"
                                 {:client_id  test-client-id
                                  :audience   "https://pigeon-scoops.us.auth0.com/api/v2/"
                                  :grant_type "password"
                                  :username   test-user
                                  :password   test-password
                                  :scope      "openid profile email"})}
       (http/post "https://pigeon-scoops.us.auth0.com/oauth/token")
       (m/decode-response-body)
       :access_token))

(defn get-management-token []
  )

(defn delete-user! [uid]
  (http/delete
    (str "https://pigeon-scoops.us.auth0.com/api/v2/users/" uid)
    {:headers {"Authorization" (str "Bearer " (get-management-token))}}))

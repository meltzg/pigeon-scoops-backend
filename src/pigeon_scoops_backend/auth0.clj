(ns pigeon-scoops-backend.auth0
  (:require [clj-http.client :as http]
            [muuntaja.core :as m]))

(def roles #{:manage-roles :manage-recipes :manage-groceries :manage-orders})

(defn get-management-token [{:keys [management-client-id management-client-secret]}]
  (->> {:content-type  :json
        :cookie-policy :standard
        :body          (m/encode "application/json"
                                 {:client_id     management-client-id
                                  :client_secret management-client-secret
                                  :audience      "https://pigeon-scoops.us.auth0.com/api/v2/"
                                  :grant_type    "client_credentials"})}
       (http/post "https://pigeon-scoops.us.auth0.com/oauth/token")
       (m/decode-response-body)
       :access_token))

(defn delete-user! [auth uid]
  (->> {:headers {"Authorization" (str "Bearer " (get-management-token auth))}}
       (http/delete
         (str "https://pigeon-scoops.us.auth0.com/api/v2/users/" uid))))

(defn create-user! [auth {:keys [connection email password]}]
  (let [token (get-management-token auth)]
    (->> {:headers          {"Authorization" (str "Bearer " token)}
          :throw-exceptions false
          :cookie-policy    :standard
          :content-type     :json
          :body             (m/encode "application/json"
                                      {:connection connection
                                       :email      email
                                       :password   password})}
         (http/post "https://pigeon-scoops.us.auth0.com/api/v2/users")
         (m/decode-response-body))))

(defn get-role-ids [token role-names]
  (->> {:headers       {"Authorization" (str "Bearer " token)}
        :cookie-policy :standard
        :content-type  :json}
       (http/get "https://pigeon-scoops.us.auth0.com/api/v2/roles")
       (m/decode-response-body)
       (filter #((set role-names) (:name %)))
       (map :id)))

(defn update-roles! [auth uid roles]
  (let [token (get-management-token auth)]
    (->> {:headers          {"Authorization" (str "Bearer " token)}
          :cookie-policy    :standard
          :content-type     :json
          :throw-exceptions false
          :body             (m/encode "application/json"
                                      {:roles (get-role-ids token (map name roles))})}
         (http/post (str "https://pigeon-scoops.us.auth0.com/api/v2/users/" uid "/roles")))))

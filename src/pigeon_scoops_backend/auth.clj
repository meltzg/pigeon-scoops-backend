(ns pigeon-scoops-backend.auth
  (:require [environ.core :refer [env]]
            [clj-http.client :as http]
            [integrant.core :as ig]
            [muuntaja.core :as m]))

(def roles #{:manage-roles :manage-recipes :manage-groceries :manage-orders :manage-menus})

(defmethod ig/expand-key :auth/auth0 [k config]
  {k (merge config (cond-> {}
                     (env :test-client-id) (conj {:test-client-id (env :test-client-id)})
                     (env :management-client-id) (conj {:management-client-id (env :management-client-id)})
                     (env :management-client-secret) (conj {:management-client-secret (env :management-client-secret)})))})

(defmethod ig/init-key :auth/auth0 [_ config]
  config)

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

(defn delete-user! [{:keys [skip-auth0-delete?] :as auth} uid]
  (if skip-auth0-delete?
    {:status 204}
    (->> {:headers {"Authorization" (str "Bearer " (get-management-token auth))}}
         (http/delete
          (str "https://pigeon-scoops.us.auth0.com/api/v2/users/" uid)))))

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

(defn get-users [auth]
  (->> {:headers {"Authorization" (str "Bearer " (get-management-token auth))}}
       (http/get
        (str "https://pigeon-scoops.us.auth0.com/api/v2/users"))
       (m/decode-response-body)))

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

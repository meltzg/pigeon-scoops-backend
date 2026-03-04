(ns pigeon-scoops-backend.account.handlers
  (:require [pigeon-scoops-backend.account.db :as account-db]
            [pigeon-scoops-backend.auth :as auth0]
            [pigeon-scoops-backend.utils :refer [with-connection]]
            [ring.util.response :as rr]))

(defn create-account! [db]
  (fn [request]
    (with-connection
      db
      (fn [conn-opts]
        (let [{:keys [sub picture] :as claims} (:claims request)
              existing-account (account-db/find-account-by-id conn-opts sub)]
          (if existing-account
            (rr/status 204)
            (do
              (account-db/create-account! conn-opts {:id sub :name (get claims "https://api.pigeon-scoops.com/email") :picture picture})
              (rr/status 201))))))))

(defn delete-account! [db]
  (fn [request]
    (let [uid (-> request :claims :sub)]
      (account-db/delete-account! db {:id uid})
      (rr/status 204))))

(defn update-roles! [auth]
  (fn [request]
    (let [uid (-> request :parameters :path :user-id)
          roles (-> request :parameters :body :roles)]
      (auth0/update-roles! auth uid roles))))

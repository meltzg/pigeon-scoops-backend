(ns pigeon-scoops-backend.account.handlers
  (:require [pigeon-scoops-backend.account.db :as account-db]
            [pigeon-scoops-backend.auth0 :as auth0]
            [ring.util.response :as rr]))

(defn create-account! [db]
  (fn [request]
    (let [{:keys [sub name picture]} (:claims request)]
      (account-db/create-account! db {:uid sub :name name :picture picture})
      (rr/status 201))))

(defn delete-account! [auth db]
  (fn [request]
    (let [uid (-> request :claims :sub)
          delete-auth0-account! (auth0/delete-user! auth uid)]
      (if (= (:status delete-auth0-account!) 204)
        (do
          (account-db/delete-account! db {:uid uid})
          (rr/status 204))
        (rr/not-found {:type    "user-not-found"
                       :message "User not found"
                       :data    (str "uid " uid)})))))

(defn update-role-to-cook! [auth]
  (fn [request]
    (let [uid (-> request :claims :sub)]
      (auth0/update-role-to-cook! auth uid))))

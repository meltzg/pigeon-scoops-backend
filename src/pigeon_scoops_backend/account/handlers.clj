(ns pigeon-scoops-backend.account.handlers
  (:require [ring.util.response :as rr]
            [pigeon-scoops-backend.account.db :as account-db]
            [pigeon-scoops-backend.auth0 :as auth0]))

(defn create-account! [db]
  (fn [request]
    (let [{:keys [sub name picture]} (:claims request)]
      (account-db/create-account! db {:uid sub :name name :picture picture})
      (rr/status 204))))

(defn delete-account! [db]
  (fn [request]
    (let [uid (-> request :claims :sub)
          delete-auth0-account! (auth0/delete-user! uid)]
      (if (= (:status delete-auth0-account!) 204)
        (do
          (account-db/delete-account! db {:uid uid})
          (rr/status 204))
        (rr/not-found {:type    "user-not-found"
                       :message "User not found"
                       :data    (str "uid " uid)})))))

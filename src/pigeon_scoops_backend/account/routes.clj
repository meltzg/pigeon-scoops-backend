(ns pigeon-scoops-backend.account.routes
  (:require [pigeon-scoops-backend.account.handlers :as account]
            [pigeon-scoops-backend.middleware :as mw]))

(defn routes [{:keys [auth] db :jdbc-url}]
  ["/account" {:swagger {:tags       ["account"]
                         :middleware [[mw/wrap-auth0]]}}
   ["" {:post   {:handler   (account/create-account! db)
                 :responses {204 {:body nil?}}
                 :summary   "Create account"}
        :delete {:handler   (account/delete-account! auth db)
                 :responses {204 {:body nil?}}
                 :summary   "Delete account"}}]])

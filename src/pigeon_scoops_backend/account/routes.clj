(ns pigeon-scoops-backend.account.routes
  (:require [pigeon-scoops-backend.account.handlers :as account]
            [pigeon-scoops-backend.middleware :as mw]))

(defn routes [{:keys [auth] db :jdbc-url :as env}]
  ["/account" {:swagger    {:tags ["account"]}
               :middleware [[mw/wrap-auth0]]}
   ["" {:post   {:handler   (account/create-account! db)
                 :responses {201 {:body nil?}}
                 :summary   "Create account"}
        :put    {:handler   (account/update-role-to-cook! auth)
                 :responses {204 {:body nil?}}
                 :summary   "Update user role to cook"}
        :delete {:handler   (account/delete-account! auth db)
                 :responses {204 {:body nil?}}
                 :summary   "Delete account"}}]])

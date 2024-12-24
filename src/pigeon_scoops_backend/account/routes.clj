(ns pigeon-scoops-backend.account.routes
  (:require [pigeon-scoops-backend.account.handlers :as account]
            [pigeon-scoops-backend.middleware :as mw]))

(defn routes [{:keys [auth] db :jdbc-url}]
  ["/account" {:swagger    {:tags ["account"]}
               :middleware [[mw/wrap-auth0]]}
   ["" {:post   {:handler   (account/create-account! db)
                 :responses {201 {:body nil?}}
                 :summary   "Create account"}
        :delete {:handler   (account/delete-account! auth db)
                 :responses {204 {:body nil?}}
                 :summary   "Delete account"}}]
   ["/:user-id" {:parameters {:path {:user-id string?}}}
    ["" {:put {:handler    (account/update-role-to-cook! auth)
               :middleware [[mw/wrap-manage-roles]]
               :responses  {204 {:body nil?}}
               :summary    "Update user role to cook"}}]]])

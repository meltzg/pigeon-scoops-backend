(ns pigeon-scoops-backend.account.routes
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.account.handlers :as account]
            [pigeon-scoops-backend.auth0 :as auth0]
            [pigeon-scoops-backend.middleware :as mw]))

(def wrap-manage-roles
  (mw/wrap-with-roles :manage-roles))

(defn routes [{:keys [auth] db :jdbc-url}]
  ["/account" {:swagger    {:tags ["account"]}
               :middleware [[mw/wrap-auth0]]}
   ["" {:post   {:handler   (account/create-account! db)
                 :responses {201 {:body nil?}}
                 :summary   "Create account"}
        :delete {:handler   (account/delete-account! auth db)
                 :responses {204 {:body nil?}}
                 :summary   "Delete account"}}]
   ["/:user-id" {:parameters {:path {:user-id string?}}
                 :put        {:handler    (account/update-roles! auth)
                              :middleware [[wrap-manage-roles]]
                              :parameters {:body {:roles [(s/and keyword? auth0/roles)]}}
                              :responses  {204 {:body nil?}}
                              :summary    "Update user role to cook"}}]])

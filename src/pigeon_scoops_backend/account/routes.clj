(ns pigeon-scoops-backend.account.routes
  (:require [pigeon-scoops-backend.middleware :as mw]
            [pigeon-scoops-backend.account.handlers :as account]))

(defn routes [{db :jdbc-url :as env}]
  ["/account" {:swagger {:tags ["account"]
                         :middleware [[mw/wrap-auth0]]}}
   ["" {:post {:handler (account/create-account! db)
               :responses {204 {:body nil?}}
               :summary "Create account"}
        :delete {:handler (account/delete-account! db)
                 :responses {204 {:body nil?}}
                 :summary "Delete account"}}]])

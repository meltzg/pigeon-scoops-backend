(ns pigeon-scoops-backend.order.routes
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.middleware :as mw]
            [pigeon-scoops-backend.order.handlers :as order]
            [pigeon-scoops-backend.order.responses :as responses]
            [pigeon-scoops-backend.units.common :as common]
            [pigeon-scoops-backend.units.mass :as mass]
            [pigeon-scoops-backend.units.volume :as volume]))

(defn routes [{db :jdbc-url}]
  ["/orders" {:swagger    {:tags ["orders"]}
              :middleware [[mw/wrap-auth0]]}
   ["" {:get  {:handler   (order/list-all-orders db)
               :responses {200 {:body [responses/order]}}
               :summary   "list of orders"}
        :post {:handler    (order/create-order! db)
               :middleware [[mw/wrap-manage-orders]]
               :parameters {:body {:note string?}}
               :responses  {201 {:body {:id uuid?}}}}}]
   ["/:order-id" {:parameters {:path {:order-id uuid?}}
                  :middleware [[(mw/wrap-owner :order-id :order) db]]}
    ["" {:get    {:handler   (order/retrieve-order db)
                  :responses {200 {:body responses/order}}
                  :summary   "Retrieve order"}
         :put    {:handler    (order/update-order! db)
                  :middleware [[mw/wrap-manage-orders]]
                  :parameters {:body {:note   string?
                                      :status (s/and keyword? responses/status)}}
                  :responses  {204 {:body nil?}}
                  :summary    "Update order"}
         :delete {:handler    (order/delete-order! db)
                  :middleware [[mw/wrap-manage-orders]]
                  :response   {204 {:body nil?}}
                  :summary    "Delete order"}}]
    ["/items" {:middleware [[mw/wrap-manage-orders]]}
     ["" {:post   {:handler    (order/create-order-item! db)
                   :parameters {:body {:recipe-id   uuid?
                                       :amount      number?
                                       :amount-unit (s/and keyword? (set (concat common/other-units
                                                                                 (keys mass/conversion-map)
                                                                                 (keys volume/conversion-map))))}}
                   :responses  {201 {:body {:id uuid?}}}
                   :summary    "Create order-item"}
          :put    {:handler    (order/update-order-item! db)
                   :parameters {:body {:id          uuid?
                                       :recipe-id   uuid?
                                       :amount      number?
                                       :amount-unit (s/and keyword? (set (concat common/other-units
                                                                                 (keys mass/conversion-map)
                                                                                 (keys volume/conversion-map))))}}
                   :responses  {204 {:body nil?}}
                   :summary    "Update order-item"}
          :delete {:handler    (order/delete-order-item! db)
                   :parameters {:body {:id uuid?}}
                   :responses  {204 {:body nil?}}
                   :summary    "delete order-item"}}]]]])

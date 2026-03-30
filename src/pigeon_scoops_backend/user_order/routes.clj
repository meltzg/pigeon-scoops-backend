(ns pigeon-scoops-backend.user-order.routes
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.grocery.responses :as grocery-responses]
            [pigeon-scoops-backend.middleware :as mw]
            [pigeon-scoops-backend.units.common :as common]
            [pigeon-scoops-backend.units.mass :as mass]
            [pigeon-scoops-backend.units.volume :as volume]
            [pigeon-scoops-backend.user-order.db :as order-db]
            [pigeon-scoops-backend.user-order.handlers :as order]
            [pigeon-scoops-backend.user-order.responses :as responses :refer [status]]
            [spec-tools.data-spec :as ds]))

(defn routes [{db :jdbc-url}]
  ["/orders" {:openapi    {:tags ["orders"]}}
   ["" {:get  {:handler   (order/list-all-orders db)
               :parameters {:query {(ds/opt :admin) boolean?}}
               :responses {200 {:body [responses/order]}}
               :summary "list of orders"}
        :post {:handler    (order/create-order! db)
               :middleware [[(mw/wrap-with-permission :create/order)]]
               :parameters {:body {:user-order/note string?}}
               :responses  {201 {:body {:id uuid?}}}}}]
   ["/:order-id" {:parameters {:path {:order-id uuid?}}
                  :middleware [[(mw/wrap-owner :order-id :user-order order-db/find-order-by-id) db]]}
    ["" {:get    {:handler   (order/retrieve-order db)
                  :responses {200 {:body responses/order}}
                  :summary   "Retrieve order"}
         :put    {:handler    (order/update-order! db)
                  :middleware [[(mw/wrap-with-permission :edit/order)]]
                  :parameters {:body {:user-order/note   string?}}
                  :responses  {204 {:body nil?}}
                  :summary    "Update order"}}]
    ["/bom" {:get {:handler   (order/retrieve-order-bom db)
                   :responses {200 {:body [grocery-responses/grocery]}}
                   :summary   "Retrieve order bom"}}]
    ["/items" {:middleware [[(mw/wrap-with-permission :edit/order)]]}
     ["" {:post   {:handler    (order/create-order-item! db)
                   :parameters {:body {:order-item/recipe-id   uuid?
                                       :order-item/amount      number?
                                       :order-item/amount-unit (s/and keyword? (set (concat common/other-units
                                                                                            (keys mass/conversion-map)
                                                                                            (keys volume/conversion-map))))}}
                   :responses  {201 {:body {:id uuid?}}}
                   :summary    "Create order-item"}}]
     ["/:order-item-id"
      {:parameters {:path {:order-item-id uuid?}}}
      ["" {:patch    {:handler    (order/update-order-item! db)
                      :parameters {:body {:order-item/recipe-id   uuid?
                                          :order-item/amount      number?
                                          :order-item/amount-unit (s/and keyword? (set (concat common/other-units
                                                                                               (keys mass/conversion-map)
                                                                                               (keys volume/conversion-map))))}}
                      :responses  {204 {:body nil?}}
                      :summary    "Update order-item"}
           :delete {:handler    (order/delete-order-item! db)
                    :responses  {204 {:body nil?}}
                    :summary    "delete order-item"}}]
      ["/status" {:patch    {:handler    (order/update-order-item-status! db)
                             :parameters {:body {:order-item/status   (s/and keyword? (set responses/status))}}
                             :responses  {204 {:body nil?}}
                             :summary    "Update order-item stus"}}]]]]])

(ns pigeon-scoops-backend.menu.routes
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.menu.handlers :as menu]
            [pigeon-scoops-backend.menu.responses :as responses]
            [pigeon-scoops-backend.middleware :as mw]
            [pigeon-scoops-backend.units.common :as common]
            [pigeon-scoops-backend.units.mass :as mass]
            [pigeon-scoops-backend.units.volume :as volume]))

(defn routes [{db :jdbc-url}]
  ["/menus" {:swagger    {:tags ["menus"]}
             :middleware [[mw/wrap-auth0]]}
   ["" {:get  {:handler   (menu/list-all-menus db)
               :responses {200 {:body [responses/menu]}}
               :summary   "list of menus"}
        :post {:handler    (menu/create-menu! db)
               :middleware [[(mw/wrap-with-permission :create/menu)]]
               :parameters {:body {:name          string?
                                   :repeats?      boolean?
                                   :duration      number?
                                   :duration-type (s/and keyword?
                                                         responses/durations)}}
               :summary    "create menu"}}]
   ["/:menu-id" {:parameters {:path {:menu-id uuid?}}}
    ["" {:get    {:handler   (menu/retrieve-menu db)
                  :responses {200 {:body responses/menu}}
                  :summary   "retrieve menu"}
         :put    {:handler    (menu/update-menu! db)
                  :middleware [[(mw/wrap-with-permission :edit/menu)]]
                  :parameters {:body {:name          string?
                                      :repeats?      boolean?
                                      :duration      number?
                                      :duration-type (s/and keyword?
                                                            responses/durations)}}
                  :summary    "Create menu"}
         :delete {:handler    (menu/delete-menu! db)
                  :middleware [[(mw/wrap-with-permission :delete/menu)]]
                  :response   {204 {:body nil?}}
                  :summary    "Delete menu"}}]
    ["/items" {:middleware [[(mw/wrap-with-permission :edit/menu)]]}
     ["" {:post   {:handler    (menu/create-menu-item! db)
                   :parameters {:body {:recipe-id uuid?}}
                   :responses  {201 {:body {:id uuid?}}}
                   :summary    "Create menu-item"}
          :put    {:handler    (menu/update-menu-item! db)
                   :parameters {:body {:id        uuid?
                                       :recipe-id uuid?}}
                   :responses  {204 {:body nil?}}
                   :summary    "Update menu-item"}
          :delete {:handler    (menu/delete-menu-item! db)
                   :parameters {:body {:id uuid?}}
                   :responses  {204 {:body nil?}}
                   :summary    "delete menu-item"}}]]
    ["/sizes" {:middleware [[(mw/wrap-with-permission :edit/menu)]]}
     ["" {:post   {:handler    (menu/create-menu-item-size! db)
                   :parameters {:body {:menu-item-id uuid?
                                       :amount       number?
                                       :amount-unit  (s/and keyword? (set (concat common/other-units
                                                                                  (keys mass/conversion-map)
                                                                                  (keys volume/conversion-map))))}}
                   :responses  {201 {:body {:id uuid?}}}
                   :summary    "Create menu-item size"}
          :put    {:handler    (menu/update-menu-item-size! db)
                   :parameters {:body {:id           uuid?
                                       :menu-item-id uuid?
                                       :amount       number?
                                       :amount-unit  (s/and keyword? (set (concat common/other-units
                                                                                  (keys mass/conversion-map)
                                                                                  (keys volume/conversion-map))))}}
                   :responses  {204 {:body nil?}}
                   :summary    "Update menu-item size"}
          :delete {:handler    (menu/delete-menu-item-size! db)
                   :parameters {:body {:id uuid?}}
                   :responses  {204 {:body nil?}}
                   :summary    "delete menu-item size"}}]]]])

(ns pigeon-scoops-backend.grocery.routes
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.grocery.handlers :as grocery]
            [pigeon-scoops-backend.grocery.responses :as responses]
            [pigeon-scoops-backend.middleware :as mw]
            [pigeon-scoops-backend.units.common :as common]
            [pigeon-scoops-backend.units.mass :as mass]
            [pigeon-scoops-backend.units.volume :as volume]
            [spec-tools.data-spec :as ds]))

(defn routes [{db :jdbc-url}]
  ["/groceries" {:swagger    {:tags ["groceries"]}
                 :middleware [[mw/wrap-auth0]]}
   ["" {:get  {:handler   (grocery/list-all-groceries db)
               :middleware [[(mw/wrap-with-permission :view/grocery)]]
               :responses {200 {:body [responses/grocery]}}
               :summary   "list of groceries"}
        :post {:handler    (grocery/create-grocery! db)
               :middleware [[(mw/wrap-with-permission :create/grocery)]]
               :parameters {:body {:name       string?
                                   :department (s/and keyword? responses/departments)}}
               :responses  {201 {:body {:id uuid?}}}}}]
   ["/:grocery-id" {:parameters {:path {:grocery-id uuid?}}}
    ["" {:get    {:handler   (grocery/retrieve-grocery db)
                  :middleware [[(mw/wrap-with-permission :view/grocery)]]
                  :responses {200 {:body responses/grocery}}
                  :summary   "Retrieve grocery"}
         :put    {:handler    (grocery/update-grocery! db)
                  :middleware [[(mw/wrap-with-permission :edit/grocery)]]
                  :parameters {:body {:name       string?
                                      :department (s/and keyword? responses/departments)}}
                  :responses  {204 {:body nil?}}
                  :summary    "Update grocery"}
         :delete {:handler    (grocery/delete-grocery! db)
                  :middleware [[(mw/wrap-with-permission :delete/grocery)]]
                  :response   {204 {:body nil?}}
                  :summary    "Delete grocery"}}]
    ["/units" {:middleware [[(mw/wrap-with-permission :edit/grocery)]]}
     ["" {:post   {:handler    (grocery/create-grocery-unit! db)
                   :parameters {:body {:source                    string?
                                       :unit-cost                 number?
                                       (ds/opt :unit-mass)        number?
                                       (ds/opt :unit-mass-type)   (s/and keyword?
                                                                         (set (keys mass/conversion-map)))
                                       (ds/opt :unit-volume)      number?
                                       (ds/opt :unit-volume-type) (s/and keyword?
                                                                         (set (keys volume/conversion-map)))
                                       (ds/opt :unit-common)      number?
                                       (ds/opt :unit-common-type) (s/and keyword? common/other-units)}}
                   :responses  {201 {:body {:id uuid?}}}
                   :summary    "Create grocery-unit"}
          :put    {:handler    (grocery/update-grocery-unit! db)
                   :parameters {:body {:id                        uuid?
                                       :source                    string?
                                       :unit-cost                 number?
                                       (ds/opt :unit-mass)        number?
                                       (ds/opt :unit-mass-type)   (s/and keyword?
                                                                         (set (keys mass/conversion-map)))
                                       (ds/opt :unit-volume)      number?
                                       (ds/opt :unit-volume-type) (s/and keyword?
                                                                         (set (keys volume/conversion-map)))
                                       (ds/opt :unit-common)      number?
                                       (ds/opt :unit-common-type) (s/and keyword? common/other-units)}}
                   :responses  {204 {:body nil?}}
                   :summary    "Update grocery-unit"}
          :delete {:handler    (grocery/delete-grocery-unit! db)
                   :parameters {:body {:id uuid?}}
                   :responses  {204 {:body nil?}}
                   :summary    "delete grocery-unit"}}]]]])

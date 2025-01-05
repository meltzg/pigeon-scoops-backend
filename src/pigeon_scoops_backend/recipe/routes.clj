(ns pigeon-scoops-backend.recipe.routes
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.middleware :as mw]
            [pigeon-scoops-backend.recipe.handlers :as recipe]
            [pigeon-scoops-backend.recipe.responses :as responses]
            [pigeon-scoops-backend.units.common :as common]
            [pigeon-scoops-backend.units.mass :as mass]
            [pigeon-scoops-backend.units.volume :as volume]
            [spec-tools.data-spec :as ds]))

(defn routes [{db :jdbc-url}]
  ["/recipes" {:swagger    {:tags ["recipes"]}
               :middleware [[mw/wrap-auth0]]}
   ["" {:get  {:handler   (recipe/list-all-recipes db)
               :responses {200 {:body responses/recipes}}
               :summary   "list of recipes"}
        :post {:handler    (recipe/create-recipe! db)
               :middleware [[mw/wrap-manage-recipes]]
               :parameters {:body {:name         string?
                                   :instructions [string?]
                                   :amount       number?
                                   :amount-unit  (s/and keyword? (set (concat common/other-units
                                                                              (keys mass/conversion-map)
                                                                              (keys volume/conversion-map))))}}
               :responses  {201 {:body {:id uuid?}}}
               :summary    "Create recipe"}}]
   ["/:recipe-id" {:parameters {:path {:recipe-id uuid?}}}
    ["" {:get    {:handler   (recipe/retrieve-recipe db)
                  :responses {200 {:body responses/recipe}}
                  :summary   "Retrieve recipe"}
         :put    {:handler    (recipe/update-recipe! db)
                  :middleware [[mw/wrap-recipe-owner db]
                               [mw/wrap-manage-recipes]]
                  :parameters {:body {:name         string?
                                      :instructions [string?]
                                      :amount       number?
                                      :amount-unit  (s/and keyword? (set (concat common/other-units
                                                                                 (keys mass/conversion-map)
                                                                                 (keys volume/conversion-map))))
                                      :public       boolean?}}
                  :responses  {204 {:body nil?}}
                  :summary    "Update recipe"}
         :delete {:handler    (recipe/delete-recipe! db)
                  :middleware [[mw/wrap-recipe-owner db]
                               [mw/wrap-manage-recipes]]
                  :response   {204 {:body nil?}}
                  :summary    "Delete recipe"}}]
    ["/favorite" {:post   {:handler   (recipe/favorite-recipe! db)
                           :responses {204 {:body nil?}}
                           :summary   "Favorite recipe"}
                  :delete {:handler  (recipe/unfavorite-recipe! db)
                           :response {204 {:body nil?}}
                           :summary  "Unfavorite recipe"}}]
    ["/ingredients" {:middleware [[mw/wrap-recipe-owner db]
                                  [mw/wrap-manage-recipes]]}
     ["" {:post   {:handler    (recipe/create-ingredient! db)
                   :middleware [[mw/wrap-recipe-owner db]]
                   :parameters {:body {(ds/opt :ingredient-grocery-id) uuid?
                                       (ds/opt :ingredient-recipe-id)  uuid?
                                       :amount                         number?
                                       :amount-unit                    (s/and keyword? (set (concat common/other-units
                                                                                                    (keys mass/conversion-map)
                                                                                                    (keys volume/conversion-map))))}}
                   :responses  {201 {:body {:id uuid?}}}
                   :summary    "Create ingredient"}
          :put    {:handler    (recipe/update-ingredient! db)
                   :middleware [[mw/wrap-recipe-owner db]]
                   :parameters {:body {:id                             uuid?
                                       (ds/opt :ingredient-grocery-id) uuid?
                                       (ds/opt :ingredient-recipe-id)  uuid?
                                       :amount                         number?
                                       :amount-unit                    (s/and keyword? (set (concat common/other-units
                                                                                                    (keys mass/conversion-map)
                                                                                                    (keys volume/conversion-map))))}}
                   :responses  {204 {:body nil?}}
                   :summary    "Update ingredient"}
          :delete {:handler    (recipe/delete-ingredient! db)
                   :middleware [[mw/wrap-recipe-owner db]]
                   :parameters {:body {:id uuid?}}
                   :responses  {204 {:body nil?}}
                   :summary    "delete ingredient"}}]]]])

(ns pigeon-scoops-backend.recipe.routes
  (:require [pigeon-scoops-backend.middleware :as mw]
            [pigeon-scoops-backend.recipe.handlers :as recipe]
            [pigeon-scoops-backend.responses :as responses]))

(defn routes [{db :jdbc-url}]
  ["/recipes" {:swagger    {:tags ["recipes"]}
               :middleware [[mw/wrap-auth0]]}
   ["" {:get  {:handler   (recipe/list-all-recipes db)
               :responses {200 {:body responses/recipes}}
               :summary   "list of recipes"}
        :post {:handler    (recipe/create-recipe! db)
               :parameters {:body {:name      string?
                                   :prep-time int?
                                   :img       string?}}
               :responses  {201 {:body {:recipe-id string?}}}
               :summary    "Create recipe"}}]
   ["/:recipe-id"
    {:parameters {:path {:recipe-id string?}}}
    [""
     {:get    {:handler    (recipe/retrieve-recipe db)
               :responses  {200 {:body responses/recipe}}
               :summary    "Retrieve recipe"}
      :put    {:handler    (recipe/update-recipe! db)
               :middleware [[mw/wrap-recipe-owner db]]
               :parameters {:body {:name      string?
                                   :prep-time int?
                                   :img       string?
                                   :public    boolean?}}
               :responses  {204 {:body nil?}}
               :summary    "Update recipe"}
      :delete {:handler    (recipe/delete-recipe! db)
               :middleware [[mw/wrap-recipe-owner db]]
               :response   {204 {:body nil?}}
               :summary    "Delete recipe"}}]
    ["/favorite" {:post   {:handler    (recipe/favorite-recipe! db)
                           :responses  {204 {:body nil?}}
                           :summary    "Favorite recipe"}
                  :delete {:handler    (recipe/unfavorite-recipe! db)
                           :response   {204 {:body nil?}}
                           :summary    "Unfavorite recipe"}}]
    ["/steps" {:post   {:handler    (recipe/create-step! db)
                        :middleware [[mw/wrap-recipe-owner db]]
                        :parameters {:body {:sort        pos-int?
                                            :description string?}}
                        :responses  {201 {:body {:step-id string?}}}
                        :summary    "Create step"}
               :put    {:handler    (recipe/update-step! db)
                        :middleware [[mw/wrap-recipe-owner db]]
                        :parameters {:body {:step-id     string?
                                            :sort        pos-int?
                                            :description string?}}
                        :summary    "Update step"
                        :responses  {204 {:body nil?}}}
               :delete {:handler    (recipe/delete-step! db)
                        :middleware [[mw/wrap-recipe-owner db]]
                        :parameters {:body {:step-id string?}}
                        :responses  {204 {:body nil?}}
                        :summary    "Delete step"}}]
    ["/ingredients" {:post   {:handler    (recipe/create-ingredient! db)
                              :middleware [[mw/wrap-recipe-owner db]]
                              :parameters {:body {:sort    pos-int?
                                                  :name    string?
                                                  :amount  pos-int?
                                                  :measure string?}}
                              :responses  {201 {:body {:ingredient-id string?}}}
                              :summary    "Create ingredient"}
                     :put    {:handler    (recipe/update-ingredient! db)
                              :middleware [[mw/wrap-recipe-owner db]]
                              :parameters {:body {:ingredient-id string?
                                                  :sort          pos-int?
                                                  :name          string?
                                                  :amount        pos-int?
                                                  :measure       string?}}
                              :responses  {204 {:body nil?}}
                              :summary    "Update ingredient"}
                     :delete {:handler    (recipe/delete-ingredient! db)
                              :middleware [[mw/wrap-recipe-owner db]]
                              :parameters {:body {:ingredient-id string?}}
                              :responses  {204 {:body nil?}}
                              :summary    "delete ingredient"}}]]])

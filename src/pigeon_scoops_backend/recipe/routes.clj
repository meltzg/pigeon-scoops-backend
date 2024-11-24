(ns pigeon-scoops-backend.recipe.routes
  (:require [pigeon-scoops-backend.recipe.handlers :as recipe]
            [pigeon-scoops-backend.responses :as responses]
            [pigeon-scoops-backend.middleware :as mw]))

(defn routes [env]
  (let [db (:jdbc-url env)]
    ["/recipes" {:swagger {:tags ["recipes"]}
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
     ["/:recipe-id" {:get    {:handler    (recipe/retrieve-recipe db)
                              :parameters {:path {:recipe-id string?}}
                              :responses  {200 {:body responses/recipe}}
                              :summary    "Retrieve recipe"}
                     :put    {:handler    (recipe/update-recipe! db)
                              :parameters {:path {:recipe-id string?}
                                           :body {:name      string?
                                                  :prep-time int?
                                                  :img       string?
                                                  :public    boolean?}}
                              :responses  {204 {:body nil}}
                              :summary    "Update recipe"}
                     :delete {:handler    (recipe/delete-recipe! db)
                              :parameters {:path {:recipe-id string?}}
                              :response   {204 {:body nil}}
                              :summary    "Delete recipe"}}]]))

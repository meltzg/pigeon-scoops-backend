(ns pigeon-scoops-backend.recipe.routes
  (:require [pigeon-scoops-backend.recipe.handlers :as recipe]
            [pigeon-scoops-backend.responses :as responses]))

(defn routes [env]
  (let [db (:jdbc-url env)]
    ["/recipes" {:swagger {:tags ["recipes"]}}
     ["" {:get {:handler   (recipe/list-all-recipes db)
                :responses {200 {:body responses/recipes}}
                :summary   "list of recipes"}}]
     ["/:recipe-id" {:get {:handler    (recipe/retrieve-recipe db)
                           :parameters {:path {:recipe-id string?}}
                           :responses  {200 {:body responses/recipe}}
                           :summary    "Retrieve recipe"}}]]))

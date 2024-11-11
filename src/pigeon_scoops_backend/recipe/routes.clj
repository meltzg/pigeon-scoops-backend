(ns pigeon-scoops-backend.recipe.routes
  (:require [pigeon-scoops-backend.recipe.handlers :as recipe]))

(defn routes [env]
  (let [db (:jdbc-url env)]
    ["/recipes" {:swagger {:tags ["recipes"]}}
     ["" {:get {:handler (recipe/list-all-recipes db)
                :summary "list of recipes"}}]
     ["/:recipe-id" {:get     {:handler (recipe/retrieve-recipe db)
                               :parameters {:path {:recipe-id string?}}
                               :summary "Retrieve recipe"}}]]))

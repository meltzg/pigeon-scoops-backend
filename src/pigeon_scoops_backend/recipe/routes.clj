(ns pigeon-scoops-backend.recipe.routes
  (:require [pigeon-scoops-backend.recipe.handlers :as recipe]
            [pigeon-scoops-backend.responses :as responses]))

(defn routes [env]
  (let [db (:jdbc-url env)]
    ["/recipes" {:swagger {:tags ["recipes"]}}
     ["" {:get  {:handler   (recipe/list-all-recipes db)
                 :responses {200 {:body responses/recipes}}
                 :summary   "list of recipes"}
          :post {:handler    (recipe/create-recipe! db)
                 :parameters {:body {:name      string?
                                     :prep-time number?
                                     :img       string?}}
                 :responses  {201 {:body {:recipe-id string?}}}
                 :summary    "Create recipe"}}]
     ["/:recipe-id" {:get {:handler    (recipe/retrieve-recipe db)
                           :parameters {:path {:recipe-id string?}}
                           :responses  {200 {:body responses/recipe}}
                           :summary    "Retrieve recipe"}}]
     "/foo" {:get (fn [_] "hello world")}]))

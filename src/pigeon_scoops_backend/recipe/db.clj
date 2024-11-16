(ns pigeon-scoops-backend.recipe.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(defn find-all-recipes [db uid]
  (with-open [conn (jdbc/get-connection db)]
    (let [public (sql/find-by-keys conn :recipe {:public true})
          drafts (when uid (sql/find-by-keys conn :recipe {:uid    uid
                                                           :public false}))]
      (merge {:public public}
             (when drafts
               {:drafts drafts})))))

(defn find-recipe-by-id [db recipe-id]
  (with-open [conn (jdbc/get-connection db)]
    (let [[recipe] (sql/find-by-keys conn :recipe {:recipe_id recipe-id})
          steps (sql/find-by-keys conn :step {:recipe_id recipe-id})
          ingredients (sql/find-by-keys conn :ingredient {:recipe_id recipe-id})]
      (when (seq recipe)
        (assoc recipe
          :recipe/steps steps
          :recipe/ingredients ingredients)))))

(defn insert-recipe! [db {:keys [recipe-id uid prep-time img name]}]
  (sql/insert! db :recipe {:recipe_id      recipe-id
                           :uid            uid
                           :name           name
                           :prep_time      prep-time
                           :img            img
                           :public         false
                           :favorite_count 0}))

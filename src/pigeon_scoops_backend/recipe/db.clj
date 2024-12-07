(ns pigeon-scoops-backend.recipe.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(defn find-all-recipes [db uid]
  (with-open [conn (jdbc/get-connection db)]
    (let [public (sql/find-by-keys conn :recipe {:public true} (:options db))
          drafts (when uid (sql/find-by-keys conn :recipe {:uid    uid
                                                           :public false} (:options db)))]
      (merge {:public public}
             (when drafts
               {:drafts drafts})))))

(defn find-recipe-by-id [db recipe-id]
  (with-open [conn (jdbc/get-connection db)]
    (let [[recipe] (sql/find-by-keys conn :recipe {:recipe_id recipe-id} (:options db))
          steps (sql/find-by-keys conn :step {:recipe_id recipe-id} (merge {:order-by [[:sort :asc]]} (:options db)))
          ingredients (sql/find-by-keys conn :ingredient {:recipe_id recipe-id} (merge {:order-by [[:sort :asc]]} (:options db)))]
      (when (seq recipe)
        (assoc recipe
          :recipe/steps steps
          :recipe/ingredients ingredients)))))

(defn insert-recipe! [db recipe]
  (sql/insert! db :recipe (assoc recipe :favorite-count 0 :public false)))

(defn update-recipe! [db recipe]
  (-> (sql/update! db :recipe recipe (select-keys recipe [:recipe-id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-recipe! [db recipe-id]
  (-> (sql/delete! db :recipe {:recipe-id recipe-id})
      ::jdbc/update-count
      (pos?)))


(defn favorite-recipe! [db {:keys [recipe-id] :as data}]
  (-> (jdbc/with-transaction
        [tx db]
        (sql/insert! tx :recipe-favorite data (:options db))
        (jdbc/execute-one! tx ["UPDATE recipe
                                SET favorite_count = favorite_count + 1
                                WHERE recipe_id = ?" recipe-id]))
      ::jdbc/update-count
      (pos?)))

(defn unfavorite-recipe! [db {:keys [recipe-id] :as data}]
  (-> (jdbc/with-transaction
        [tx db]
        (sql/delete! tx :recipe-favorite data (:options db))
        (jdbc/execute-one! tx ["UPDATE recipe
                                SET favorite_count = favorite_count - 1
                                WHERE recipe_id = ?" recipe-id]))
      ::jdbc/update-count
      (pos?)))

(defn insert-step! [db step]
  (sql/insert! db, :step step))

(defn update-step! [db step]
  (-> (sql/update! db :step step (select-keys step [:recipe-id :step-id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-step! [db step]
  (-> (sql/delete! db :step step)
      ::jdbc/update-count
      (pos?)))

(defn insert-ingredient! [db ingredient]
  (sql/insert! db, :ingredient ingredient))

(defn update-ingredient! [db ingredient]
  (-> (sql/update! db :ingredient ingredient (select-keys ingredient [:recipe-id :ingredient-id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-ingredient! [db ingredient]
  (-> (sql/delete! db :ingredient ingredient)
      ::jdbc/update-count
      (pos?)))

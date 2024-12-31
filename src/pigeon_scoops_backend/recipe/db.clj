(ns pigeon-scoops-backend.recipe.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(defn keywordize-amount-unit [recipe]
  (-> recipe
      (assoc :recipe/amount-unit (keyword (:recipe/amount-unit-type recipe)
                                          (:recipe/amount-unit recipe)))))

(defn find-all-recipes [db uid]
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          public (sql/find-by-keys conn-opts :recipe {:public true})
          private (when uid (sql/find-by-keys conn-opts :recipe {:user-id uid
                                                                 :public  false}))]
      (merge {:public (map keywordize-amount-unit public)}
             (when private
               {:private (map keywordize-amount-unit private)})))))

(defn find-recipe-by-id [db recipe-id]
  (with-open [conn (jdbc/get-connection db)]
    (let [conn-opts (jdbc/with-options conn (:options db))
          [recipe] (sql/find-by-keys conn-opts :recipe {:id recipe-id})
          ingredients (sql/find-by-keys conn-opts :ingredient {:recipe-id recipe-id})
          favorite-count (count (sql/find-by-keys conn-opts :recipe-favorite {:recipe-id recipe-id}))]
      (when (seq recipe)
        (-> recipe
            (keywordize-amount-unit)
            (assoc
              :recipe/ingredients ingredients
              :recipe/favorite-count favorite-count))))))

(defn insert-recipe! [db recipe]
  (println (assoc recipe :public false
                         :amount-unit (name (:amount-unit recipe))
                         :amount-unit-type (namespace (:amount-unit recipe))))
  (sql/insert! db :recipe (assoc recipe :public false
                                        :instructions (into-array String (:instructions recipe))
                                        :amount-unit (name (:amount-unit recipe))
                                        :amount-unit-type (namespace (:amount-unit recipe)))))

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

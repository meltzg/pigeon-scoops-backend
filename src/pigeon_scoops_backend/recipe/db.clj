(ns pigeon-scoops-backend.recipe.db
  (:require [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.recipe.utils :as utils]
            [pigeon-scoops-backend.utils :refer [db-str->keyword
                                                 keyword->db-str
                                                 with-connection]]))

(defn find-recipe-favorite-counts [db recipe-ids]
  (when (seq recipe-ids)
    (->> (sql/query db (-> (h/select :recipe-id :%count.*)
                           (h/from :recipe-favorite)
                           (h/where [:in :recipe-id recipe-ids])
                           (h/group-by :recipe-id)
                           (hsql/format)))
         (map (comp vec vals))
         (into {}))))

(defn find-all-recipes
  ([db uid]
   (find-all-recipes db uid false))
  ([db uid include-deleted?]
   (with-connection
     db
     (fn [conn-opts]
       (let [condition (when-not include-deleted? {:deleted false})
             public (sql/find-by-keys conn-opts :recipe (merge condition {:public true}))
             private (when uid (sql/find-by-keys conn-opts :recipe (merge condition {:user-id uid
                                                                                     :public  false})))
             favorite-counts (find-recipe-favorite-counts conn-opts (concat (map :recipe/id public)
                                                                            (map :recipe/id private)))]
         (merge {:public (map #(db-str->keyword (assoc % :recipe/favorite-count (or (get favorite-counts (:recipe/id %)) 0))
                                                :recipe/amount-unit)
                              public)}
                (when private
                  {:private (map #(db-str->keyword (assoc % :recipe/favorite-count (or (get favorite-counts (:recipe/id %)) 0))
                                                   :recipe/amount-unit)
                                 private)})))))))


(defn find-recipe-by-id [db recipe-id]
  (with-connection
    db
    (fn [conn-opts]
      (let [[recipe] (sql/find-by-keys conn-opts :recipe {:id recipe-id})
            ingredients (sql/query conn-opts (-> (h/select :ingredient/* :recipe/name :grocery/name)
                                                 (h/from :ingredient)
                                                 (h/left-join :recipe [:= :ingredient/ingredient-recipe-id :recipe/id])
                                                 (h/left-join :grocery [:= :ingredient/ingredient-grocery-id :grocery/id])
                                                 (h/where [:= :ingredient/recipe-id recipe-id])
                                                 (hsql/format)))
            favorite-count (count (sql/find-by-keys conn-opts :recipe-favorite {:recipe-id recipe-id}))]
        (when (seq recipe)
          (-> recipe
              (db-str->keyword :recipe/amount-unit)
              (assoc
                :recipe/ingredients (map #(db-str->keyword (into {} (remove (comp nil? val) %)) :ingredient/amount-unit) ingredients)
                :recipe/favorite-count favorite-count)))))))

(defn insert-recipe! [db recipe]
  (sql/insert! db :recipe (-> recipe
                              (keyword->db-str :amount-unit)
                              (assoc :public false
                                     :instructions (into-array String (:instructions recipe))))))

(defn update-recipe! [db recipe]
  (-> (sql/update! db :recipe (-> recipe
                                  (keyword->db-str :amount-unit)
                                  (update :instructions (partial into-array String)))
                   (select-keys recipe [:id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-recipe! [db recipe-id]
  (-> (sql/update! db :recipe {:deleted true} {:id recipe-id})
      ::jdbc/update-count
      (pos?)))


(defn favorite-recipe! [db data]
  (sql/insert! db :recipe-favorite data (:options db)))

(defn unfavorite-recipe! [db data]
  (sql/delete! db :recipe-favorite data (:options db)))

(defn insert-ingredient! [db ingredient]
  (sql/insert! db, :ingredient (keyword->db-str ingredient :amount-unit)))

(defn update-ingredient! [db ingredient]
  (-> (sql/update! db :ingredient (keyword->db-str
                                    (cond-> ingredient
                                            (some? (:ingredient-grocery-id ingredient))
                                            (assoc :ingredient-recipe-id nil)
                                            (some? (:ingredient-recipe-id ingredient))
                                            (assoc :ingredient-grocery-id nil))
                                    :amount-unit)
                   (select-keys ingredient [:id]))
      ::jdbc/update-count
      (pos?)))

(defn delete-ingredient! [db ingredient]
  (-> (sql/delete! db :ingredient ingredient)
      ::jdbc/update-count
      (pos?)))

(defn ingredient-bom [db {:recipe/keys [id amount amount-unit]}]
  (with-connection
    db
    (fn [conn-opts]
      (loop [curr-recipe-ingredients [{:ingredient/ingredient-recipe-id id
                                       :ingredient/amount               amount
                                       :ingredient/amount-unit          amount-unit}]
             curr-grocery-ingredients []]
        (if-not (seq curr-recipe-ingredients)
          (utils/combine-ingredients curr-grocery-ingredients)
          (let [{:ingredient/keys [ingredient-recipe-id amount amount-unit]} (first curr-recipe-ingredients)
                {:keys [recipe-ingredients grocery-ingredients]}
                (->> (utils/scale-recipe
                       (find-recipe-by-id conn-opts ingredient-recipe-id)
                       amount
                       amount-unit)
                     :recipe/ingredients
                     (group-by #(if (some? (:ingredient/ingredient-recipe-id %))
                                  :recipe-ingredients
                                  :grocery-ingredients)))]
            (recur (concat (rest curr-recipe-ingredients) recipe-ingredients)
                   (concat curr-grocery-ingredients grocery-ingredients))))))))

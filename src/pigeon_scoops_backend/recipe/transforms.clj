(ns pigeon-scoops-backend.recipe.transforms
  (:require [pigeon-scoops-backend.units.common :as common]))

(defn scale-recipe [recipe amount amount-unit]
  (let [scale-factor (common/scale-factor (:recipe/amount recipe)
                                          (:recipe/amount-unit recipe)
                                          amount
                                          amount-unit)]
    (when scale-factor
      (-> recipe
          (assoc :recipe/amount amount
                 :recipe/amount-unit amount-unit)
          (update :recipe/ingredients
                  #(map (fn [i]
                          (update i :ingredient/amount * scale-factor))
                        %))))))

(defn combine-ingredients [ingredients]
  (as-> ingredients ?
        (group-by (comp #(update % :ingredient/amount-unit namespace)
                        #(select-keys % [:ingredient/amount-unit
                                         :ingredient/ingredient-grocery-id
                                         :ingredient/ingredient-recipe-id]))
                  ?)
        (update-vals ? (partial reduce
                                (fn [{sink-amount-unit :ingredient/amount-unit :as acc}
                                     {:ingredient/keys [amount amount-unit]}]
                                  (let [scaled-amount (common/convert amount amount-unit sink-amount-unit)]
                                    (update acc :ingredient/amount + scaled-amount)))))
        (vals ?)))

(defn anonymize-mystery-recipe [user-id recipe]
  (if (and (:recipe/is-mystery recipe) (not= (:recipe/user-id recipe) user-id))
    (-> recipe
        (assoc :recipe/name "?????"
               :recipe/description "?????")
        (dissoc :recipe/instructions :recipe/ingredients))
    recipe))

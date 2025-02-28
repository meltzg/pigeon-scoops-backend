(ns pigeon-scoops-backend.recipe.utils
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

(comment
  (combine-ingredients [{:ingredient/ingredient-recipe-id "good stuff"
                         :ingredient/amount               1
                         :ingredient/amount-unit          :mass/kg}
                        {:ingredient/ingredient-recipe-id "good stuff"
                         :ingredient/amount               2.2
                         :ingredient/amount-unit          :mass/lb}
                        {:ingredient/ingredient-recipe-id "good stuff"
                         :ingredient/amount               2
                         :ingredient/amount-unit          :volume/gal}
                        {:ingredient/ingredient-recipe-id "other stuff"
                         :ingredient/amount               3
                         :ingredient/amount-unit          :mass/kg}
                        {:ingredient/ingredient-recipe-id "other stuff"
                         :ingredient/amount               3
                         :ingredient/amount-unit          :mass/kg}]))
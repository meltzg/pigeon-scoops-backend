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

(defn anonymize-mystery-recipe [recipe]
  (if (:recipe/is-mystery recipe)
    (-> recipe
        (assoc :recipe/name "?????"
               :recipe/description "?????")
        (dissoc :recipe/instructions :recipe/ingredients))
    recipe))

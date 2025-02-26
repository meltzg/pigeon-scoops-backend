(ns pigeon-scoops-backend.grocery.utils
  (:require [pigeon-scoops-backend.units.common :as common]))

(defn get-minimal-unit [units amount amount-unit]
  (let [[quantity-key unit-key] (->> amount-unit
                                     (namespace)
                                     (keyword)
                                     (get {:mass   [:grocery-unit/unit-mass :grocery-unit/unit-mass-type]
                                           :volume [:grocery-unit/unit-volume :grocery-unit/unit-volume-type]
                                           :common [:grocery-unit/unit-common :grocery-unit/unit-common-type]}))
        comparable-amount (common/to-comparable amount amount-unit)]
    (->> units
         (map #(assoc % :comparable-amount (common/to-comparable (quantity-key %) (unit-key %))))
         (sort-by :comparable-amount)
         (filter #(and (some? (common/convert amount amount-unit (unit-key %)))
                       (<= comparable-amount (:comparable-amount %))))
         (first))))

(defn get-quantized-units [])

(comment
  (get-minimal-unit [{:grocery-unit/unit-mass 5 :grocery-unit/unit-mass-type :mass/kg}
                     {:grocery-unit/unit-mass 5 :grocery-unit/unit-mass-type :mass/g}] 4 :mass/lb))

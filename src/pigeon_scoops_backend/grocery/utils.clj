(ns pigeon-scoops-backend.grocery.utils
  (:require [pigeon-scoops-backend.units.common :as common]))

(defn get-unit-keys [amount-unit]
  (->> amount-unit
       (namespace)
       (keyword)
       (get {:mass   [:grocery-unit/unit-mass :grocery-unit/unit-mass-type]
             :volume [:grocery-unit/unit-volume :grocery-unit/unit-volume-type]
             :common [:grocery-unit/unit-common :grocery-unit/unit-common-type]})))

(defn get-optimal-unit [units amount amount-unit]
  (let [[quantity-key unit-key] (get-unit-keys amount-unit)
        comparable-amount (common/to-comparable amount amount-unit)]
    (->> units
         (map #(assoc % :comparable-amount (common/to-comparable (quantity-key %) (unit-key %))))
         (filter (comp some? :comparable-amount))
         (sort-by #(Math/abs ^Double (- comparable-amount (:comparable-amount %))))
         (first))))

(defn get-quantized-units [units amount amount-unit]
  (let [[quantity-key unit-key] (get-unit-keys amount-unit)
        needed-units (loop [needed-units []
                            remaining-amount amount]
                       (let [unit (get-optimal-unit units remaining-amount amount-unit)
                             remaining-amount (if unit
                                                (first (common/add-amounts remaining-amount amount-unit
                                                                           (- (quantity-key unit)) (unit-key unit)))
                                                remaining-amount)]
                         (cond (nil? unit) needed-units
                               (<= remaining-amount 0) (conj needed-units unit)
                               :else (recur (conj needed-units unit) remaining-amount))))]
    (->> needed-units
         (group-by identity)
         (vals)
         (map (fn [group]
                (assoc (first group) :grocery-unit/quantity (count group)))))))

(comment
  (get-optimal-unit [{:grocery-unit/unit-mass 5 :grocery-unit/unit-mass-type :mass/kg}
                     {:grocery-unit/unit-mass 5 :grocery-unit/unit-mass-type :mass/g}] 5100 :volume/gal))

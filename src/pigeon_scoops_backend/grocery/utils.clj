(ns pigeon-scoops-backend.grocery.utils
  (:require [pigeon-scoops-backend.units.common :as common]))

(defn get-grocery-unit-for-amount [{:grocery/keys [units]} amount amount-unit]
  (let [[unit-key unit-type-key] (->> amount-unit
                                     (namespace)
                                     (keyword)
                                     (get {:mass [:grocery-unit/unit-mass :grocery-unit/unit-mass-type]
                                           :volume[:grocery-unit/unit-volume :grocery-unit/unit-volume-type]
                                           :common [:grocery-unit/unit-common :grocery-unit/unit-common-type]}))]
    [unit-key unit-type-key]))

(comment
  (get-grocery-unit-for-amount [] 4 :common/unit))

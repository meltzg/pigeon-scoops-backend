(ns pigeon-scoops-backend.grocery.utils-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.grocery.utils :as utils]))

(def units [{:grocery-unit/unit-mass 5
             :grocery-unit/unit-mass-type :mass/kg
             :grocery-unit/unit-cost 50}
            {:grocery-unit/unit-mass 5
             :grocery-unit/unit-mass-type :mass/g
             :grocery-unit/unit-cost 0.05}])

(deftest get-optimal-unit-test
  (testing "given a set of units and a needed amount, retrieve the unit closest in amount"
    (are [amount amount-unit expected]
      (= (dissoc (utils/get-optimal-unit units amount amount-unit) :comparable-amount)
         expected)
      2600 :mass/g {:grocery-unit/unit-mass 5
                    :grocery-unit/unit-mass-type :mass/kg
                    :grocery-unit/unit-cost 50}
      100 :mass/g {:grocery-unit/unit-mass 5
                   :grocery-unit/unit-mass-type :mass/g
                   :grocery-unit/unit-cost 0.05}
      100 :volume/gal nil)))

(deftest units-for-amount-test
  (testing "given a set of units and a needed amount, we can get a list of the necessary units and their quantities"
    (are [amount amount-unit expected]
      (= (map #(select-keys % (keys (first expected))) (utils/units-for-amount units amount amount-unit))
         expected)
      5105 :mass/g [{:grocery-unit/unit-mass 5
                     :grocery-unit/unit-mass-type :mass/kg
                     :grocery-unit/unit-cost 50
                     :grocery-unit/quantity 1}
                    {:grocery-unit/unit-mass 5
                     :grocery-unit/unit-mass-type :mass/g
                     :grocery-unit/unit-cost 0.05
                     :grocery-unit/quantity 21}]
      100 :volume/gal [])))

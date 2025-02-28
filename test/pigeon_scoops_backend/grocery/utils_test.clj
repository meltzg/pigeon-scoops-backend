(ns pigeon-scoops-backend.grocery.utils-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.grocery.utils :as utils]))

(def units [{:grocery-unit/unit-mass      5
             :grocery-unit/unit-mass-type :mass/kg
             :grocery-unit/unit-cost      50}
            {:grocery-unit/unit-mass      5
             :grocery-unit/unit-mass-type :mass/g
             :grocery-unit/unit-cost      0.05}])

(deftest get-optimal-unit-test
  (testing "given a set of units and a needed amount, retrieve the unit closest in amount"
    (are [amount amount-unit expected]
      (= (dissoc (utils/get-optimal-unit units amount amount-unit) :comparable-amount)
         expected)
      2600 :mass/g {:grocery-unit/unit-mass      5
                    :grocery-unit/unit-mass-type :mass/kg
                    :grocery-unit/unit-cost      50}
      100 :mass/g {:grocery-unit/unit-mass      5
                   :grocery-unit/unit-mass-type :mass/g
                   :grocery-unit/unit-cost      0.05}
      100 :volume/gal nil)))

(deftest units-for-amount-test
  (testing "given a set of units and a needed amount, we can get a list of the necessary units and their quantities"
    (are [amount amount-unit expected]
      (= (map #(select-keys % (keys (first expected))) (utils/units-for-amount units amount amount-unit))
         expected)
      5105 :mass/g [{:grocery-unit/unit-mass      5
                     :grocery-unit/unit-mass-type :mass/kg
                     :grocery-unit/unit-cost      50
                     :grocery-unit/quantity       1}
                    {:grocery-unit/unit-mass      5
                     :grocery-unit/unit-mass-type :mass/g
                     :grocery-unit/unit-cost      0.05
                     :grocery-unit/quantity       21}]
      100 :volume/gal [])))

(deftest grocery-for-amount-test
  (testing "given a grocery with units and an amount, we can get an updated grocery with purchase data"
    (are [amount amount-unit expected]
      (= (update (utils/grocery-for-amount {:grocery/units [{:grocery-unit/unit-mass 1 :grocery-unit/unit-mass-type :mass/kg}
                                                            {:grocery-unit/unit-mass 250 :grocery-unit/unit-mass-type :mass/g}]}
                                           amount amount-unit)
                 :grocery/units (partial map #(dissoc % :comparable-amount)))
         expected)
      2400 :mass/g #:grocery{:units           '({:grocery-unit/unit-mass      1,
                                                 :grocery-unit/unit-mass-type :mass/kg,
                                                 :grocery-unit/quantity       2}
                                                {:grocery-unit/unit-mass      250,
                                                 :grocery-unit/unit-mass-type :mass/g,
                                                 :grocery-unit/quantity       2}),
                             :required-amount 2400,
                             :required-unit   :mass/g,
                             :purchase-amount 5/2,
                             :purchase-unit   :mass/kg,
                             :waste-ratio     1/25}
      2400 :volume/gal #:grocery{:units           '(),
                                 :required-amount 2400,
                                 :required-unit   :volume/gal,
                                 :purchase-amount nil,
                                 :purchase-unit   nil,
                                 :waste-ratio     nil})))

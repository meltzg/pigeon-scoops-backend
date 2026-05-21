(ns pigeon-scoops-backend.utils-test
  (:require [clojure.test :refer [deftest is testing]]
            [pigeon-scoops-backend.utils :as utils]))

(deftest combine-amounts-test
  (testing "A list of amounts can be combined by discriminant keys and unit type"
    (is (= (utils/combine-amounts [{:entity/entity-discriminant-key-1 "good stuff"
                                    :entity/amount               1
                                    :entity/amount-unit          :mass/kg}
                                   {:entity/entity-discriminant-key-1 "good stuff"
                                    :entity/amount               2.2
                                    :entity/amount-unit          :mass/lb}
                                   {:entity/entity-discriminant-key-1 "good stuff"
                                    :entity/amount               2
                                    :entity/amount-unit          :volume/gal}
                                   {:entity/entity-discriminant-key-1 "other stuff"
                                    :entity/amount               3
                                    :entity/amount-unit          :mass/kg}
                                   {:entity/entity-discriminant-key-1 "other stuff"
                                    :entity/amount               3
                                    :entity/amount-unit          :mass/kg}
                                   {:entity/entity-discriminant-key-2 "other stuff"
                                    :entity/amount               3
                                    :entity/amount-unit          :mass/kg}
                                   {:entity/entity-discriminant-key-2 "other stuff"
                                    :entity/amount               3
                                    :entity/amount-unit          :mass/kg}
                                   {:entity/entity-discriminant-key-1 "good stuff"
                                    :entity/entity-discriminant-key-2 "other stuff"
                                    :entity/amount               2
                                    :entity/amount-unit          :volume/gal}
                                   {:entity/entity-discriminant-key-1 "good stuff"
                                    :entity/entity-discriminant-key-2 "other stuff"
                                    :entity/amount               2
                                    :entity/amount-unit          :volume/qt}]
                                  :entity/amount
                                  :entity/amount-unit
                                  :entity/entity-discriminant-key-1
                                  :entity/entity-discriminant-key-2)
           [{:entity/entity-discriminant-key-1 "good stuff"
             :entity/amount               1.9979024
             :entity/amount-unit          :mass/kg}
            {:entity/entity-discriminant-key-1 "good stuff"
             :entity/amount               2
             :entity/amount-unit          :volume/gal}
            {:entity/entity-discriminant-key-1 "other stuff"
             :entity/amount               6
             :entity/amount-unit          :mass/kg}
            {:entity/entity-discriminant-key-2 "other stuff"
             :entity/amount               6
             :entity/amount-unit          :mass/kg}
            {:entity/entity-discriminant-key-1 "good stuff"
             :entity/entity-discriminant-key-2 "other stuff"
             :entity/amount               2.5
             :entity/amount-unit          :volume/gal}]))))

(ns pigeon-scoops-backend.recipe.transforms-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.recipe.transforms :as transforms]))


(def recipe
  {:recipe/name         "a spicy meatball"
   :recipe/amount       1
   :recipe/amount-unit  :volume/qt
   :recipe/source       "the book"
   :recipe/instructions ["make them"]
   :recipe/ingredients  [{:ingredient/id          "foo"
                          :ingredient/amount      3
                          :ingredient/amount-unit :mass/kg}
                         {:ingredient/id          "bar"
                          :ingredient/amount      2
                          :ingredient/amount-unit :common/unit}]})


(deftest scale-recipe-test
  (testing "A recipe can be scaled up and down"
    (are [recipe amount amount-unit expected]
      (= (transforms/scale-recipe recipe amount amount-unit) expected)
      recipe 3 :volume/qt {:recipe/name         "a spicy meatball"
                           :recipe/amount       3
                           :recipe/amount-unit  :volume/qt
                           :recipe/source       "the book"
                           :recipe/instructions ["make them"]
                           :recipe/ingredients  [{:ingredient/id          "foo"
                                                  :ingredient/amount      9.0
                                                  :ingredient/amount-unit :mass/kg}
                                                 {:ingredient/id          "bar"
                                                  :ingredient/amount      6.0
                                                  :ingredient/amount-unit :common/unit}]}
      recipe 0.5 :volume/qt {:recipe/name         "a spicy meatball"
                             :recipe/amount       0.5
                             :recipe/amount-unit  :volume/qt
                             :recipe/source       "the book"
                             :recipe/instructions ["make them"]
                             :recipe/ingredients  [{:ingredient/id          "foo"
                                                    :ingredient/amount      1.5
                                                    :ingredient/amount-unit :mass/kg}
                                                   {:ingredient/id          "bar"
                                                    :ingredient/amount      1.0
                                                    :ingredient/amount-unit :common/unit}]}
      recipe 4 :volume/c {:recipe/name         "a spicy meatball"
                          :recipe/amount       4
                          :recipe/amount-unit  :volume/c
                          :recipe/source       "the book"
                          :recipe/instructions ["make them"]
                          :recipe/ingredients  [{:ingredient/id          "foo"
                                                 :ingredient/amount      3.0
                                                 :ingredient/amount-unit :mass/kg}
                                                {:ingredient/id          "bar"
                                                 :ingredient/amount      2.0
                                                 :ingredient/amount-unit :common/unit}]})))

(deftest combine-ingredients-test
  (testing "A list of ingredients can be combined by ingredient and unit class"
    (is (= (transforms/combine-ingredients [{:ingredient/ingredient-recipe-id "good stuff"
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
                                             :ingredient/amount-unit          :mass/kg}])
           [{:ingredient/ingredient-recipe-id "good stuff"
             :ingredient/amount               1.9979024
             :ingredient/amount-unit          :mass/kg}
            {:ingredient/ingredient-recipe-id "good stuff"
             :ingredient/amount               2
             :ingredient/amount-unit          :volume/gal}
            {:ingredient/ingredient-recipe-id "other stuff"
             :ingredient/amount               6
             :ingredient/amount-unit          :mass/kg}]))))
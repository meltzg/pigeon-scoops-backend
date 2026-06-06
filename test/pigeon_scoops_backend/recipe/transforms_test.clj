(ns pigeon-scoops-backend.recipe.transforms-test
  (:require [clojure.test :refer [are deftest is testing]]
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

(deftest anonymize-mystery-recipe-test
  (testing "non-mystery recipe is returned unchanged"
    (is (= recipe (transforms/anonymize-mystery-recipe recipe))))
  (testing "nil is returned unchanged"
    (is (nil? (transforms/anonymize-mystery-recipe nil))))
  (testing "mystery recipe hides name, description, and removes instructions and ingredients"
    (let [mystery (assoc recipe :recipe/is-mystery true
                         :recipe/description "secret")
          result  (transforms/anonymize-mystery-recipe mystery)]
      (is (= "?????" (:recipe/name result)))
      (is (= "?????" (:recipe/description result)))
      (is (not (contains? result :recipe/instructions)))
      (is (not (contains? result :recipe/ingredients))))))

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

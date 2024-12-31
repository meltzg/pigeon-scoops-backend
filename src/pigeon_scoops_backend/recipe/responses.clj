(ns pigeon-scoops-backend.recipe.responses
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.units.common :as common]
            [pigeon-scoops-backend.units.mass :as mass]
            [pigeon-scoops-backend.units.volume :as volume]
            [spec-tools.data-spec :as ds]))

(def ingredient
  {:ingredient/ingredient-id       string?
   :ingredient/sort                int?
   :ingredient/name                string?
   :ingredient/amount              int?
   :ingredient/measure             string?
   (ds/opt :ingredient/grocery-id) uuid?
   (ds/opt :ingredient/recipe-id)  uuid?})

(def recipe
  {:recipe/public                boolean?
   :recipe/favorite-count        int?
   :recipe/id                    uuid?
   :recipe/name                  string?
   :recipe/user-id               string?
   :recipe/amount                number?
   :recipe/amount-unit           (s/and keyword? (set (concat common/other-units
                                                              (keys mass/conversion-map)
                                                              (keys volume/conversion-map))))
   (ds/opt :recipe/instructions) [string?]
   (ds/opt :recipe/ingredients)  [ingredient]})

(def recipes
  {:public           [recipe]
   (ds/opt :private) [recipe]})

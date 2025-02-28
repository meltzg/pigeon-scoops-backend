(ns pigeon-scoops-backend.grocery.responses
  (:require [clojure.spec.alpha :as s]
            [spec-tools.data-spec :as ds]))

(def departments
  #{:department/dairy
    :department/produce
    :department/grocery})

(def grocery-unit
  {:grocery-unit/id                        uuid?
   :grocery-unit/grocery-id                uuid?
   :grocery-unit/source                    string?
   :grocery-unit/unit-cost                 number?
   (ds/opt :grocery-unit/unit-mass)        number?
   (ds/opt :grocery-unit/unit-mass-type)   keyword?
   (ds/opt :grocery-unit/unit-volume)      number?
   (ds/opt :grocery-unit/unit-volume-type) keyword?
   (ds/opt :grocery-unit/unit-common)      number?
   (ds/opt :grocery-unit/unit-common-type) keyword?
   (ds/opt :grocery-unit/quantity)         int?})

(def grocery
  {:grocery/id                       uuid?
   :grocery/name                     string?
   :grocery/department               keyword?
   (ds/opt :grocery/units)           [grocery-unit]
   (ds/opt :grocery/required-amount) (s/nilable number?)
   (ds/opt :grocery/required-unit)   (s/nilable keyword?)
   (ds/opt :grocery/purchase-amount) (s/nilable number?)
   (ds/opt :grocery/purchase-unit)   (s/nilable keyword?)
   (ds/opt :grocery/waste-ratio)     (s/nilable number?)})

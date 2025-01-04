(ns pigeon-scoops-backend.grocery.responses
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.units.common :as common]
            [pigeon-scoops-backend.units.mass :as mass]
            [pigeon-scoops-backend.units.volume :as volume]
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
   (ds/opt :grocery-unit/unit-mass-type)   (s/and keyword?
                                                  (set (keys mass/conversion-map)))
   (ds/opt :grocery-unit/unit-volume)      number?
   (ds/opt :grocery-unit/unit-volume-type) (s/and keyword?
                                                  (set (keys volume/conversion-map)))
   (ds/opt :grocery-unit/unit-common)      number?
   (ds/opt :grocery-unit/unit-common-type) (s/and keyword? common/other-units)})

(def grocery
  {:grocery/id             uuid?
   :grocery/name           string?
   :grocery/department     (s/and keyword? departments)
   (ds/opt :grocery/units) [grocery-unit]})

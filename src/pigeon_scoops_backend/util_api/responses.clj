(ns pigeon-scoops-backend.util-api.responses
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.auth0 :refer [roles]]
            [pigeon-scoops-backend.grocery.responses :refer [departments]]
            [pigeon-scoops-backend.units.common :as common]
            [pigeon-scoops-backend.units.mass :as mass]
            [pigeon-scoops-backend.units.volume :as volume]
            [pigeon-scoops-backend.user-order.responses :refer [status]]))

(def constants
  {:constants/unit-types     [(s/and keyword?
                                     (set (concat common/other-units
                                                  (keys mass/conversion-map)
                                                  (keys volume/conversion-map))))]
   :constants/departments    [(s/and keyword? departments)]
   :constants/roles          [(s/and keyword? roles)]
   :constants/order-statuses [(s/and keyword? status)]})


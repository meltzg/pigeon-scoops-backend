(ns pigeon-scoops-backend.order.responses
  (:require [clojure.spec.alpha :as s]
            [pigeon-scoops-backend.units.common :as common]
            [pigeon-scoops-backend.units.mass :as mass]
            [pigeon-scoops-backend.units.volume :as volume]
            [spec-tools.data-spec :as ds]))

(def status
  #{:status/draft
    :status/submitted
    :status/in-progress
    :status/complete})

(def order-item
  {:order-item/id                   uuid?
   :order-item/recipe-id            uuid?
   :order-item/order-id             uuid?
   :order-item/unit-cost            number?
   (ds/opt :order-item/amount)      number?
   (ds/opt :order-item/amount-unit) (s/and keyword?
                                           (set (concat common/other-units
                                                        (keys mass/conversion-map)
                                                        (keys volume/conversion-map))))})

(def order
  {:order/id             uuid?
   :order/note           string?
   :order/user-id        string?
   :order/status         (s/and keyword? status)
   (ds/opt :order/units) [order-item]})

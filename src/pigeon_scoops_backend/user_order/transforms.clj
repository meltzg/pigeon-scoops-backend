(ns pigeon-scoops-backend.user-order.transforms
  (:require
   [pigeon-scoops-backend.user-order.responses :refer [status]]))

(defn infer-order-status [order-items]
  (if (empty? order-items)
    :status/draft
    (->> order-items
         (map :order-item/status)
         (apply max-key #(- (.indexOf status %))))))

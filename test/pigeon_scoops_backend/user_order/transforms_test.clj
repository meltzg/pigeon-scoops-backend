(ns pigeon-scoops-backend.user-order.transforms-test
  (:require [clojure.test :refer [are deftest is testing]]
            [pigeon-scoops-backend.user-order.transforms :refer [can-submit? infer-order-status]]))

(deftest infer-order-status-test
  (testing "infer-order-status returns the status with the minimum index among all items"
    (are [expected items] (= expected (infer-order-status items))
      :status/draft [{:order-item/status :status/draft}]
      :status/submitted [{:order-item/status :status/submitted}]
      :status/in-progress [{:order-item/status :status/in-progress}]
      :status/complete [{:order-item/status :status/complete}]

      ;; mixed items: the "earliest" status should win
      :status/draft [{:order-item/status :status/draft} {:order-item/status :status/submitted}]
      :status/draft [{:order-item/status :status/draft} {:order-item/status :status/complete}]
      :status/submitted [{:order-item/status :status/submitted} {:order-item/status :status/in-progress}]
      :status/submitted [{:order-item/status :status/submitted} {:order-item/status :status/complete}]
      :status/in-progress [{:order-item/status :status/in-progress} {:order-item/status :status/complete}]
      :status/complete [{:order-item/status :status/complete} {:order-item/status :status/complete}]

      ;; all same
      :status/draft [{:order-item/status :status/draft} {:order-item/status :status/draft}]))
  (testing "infer-order-status handles more complex mixed scenarios"
    (is (= :status/draft (infer-order-status [{:order-item/status :status/complete}
                                              {:order-item/status :status/in-progress}
                                              {:order-item/status :status/submitted}
                                              {:order-item/status :status/draft}])))
    (is (= :status/submitted (infer-order-status [{:order-item/status :status/complete}
                                                  {:order-item/status :status/in-progress}
                                                  {:order-item/status :status/submitted}]))))
  (testing "infer-order-status returns :status/draft when the list is empty"
    (is (= :status/draft (infer-order-status [])))))

(deftest can-submit?-test
  (testing "unlimited (negative) available-quantity always allows submission"
    (are [qty] (can-submit? -1 qty)
      0
      1
      100))
  (testing "limited available-quantity allows submission when enough is available"
    (are [available qty] (can-submit? available qty)
      5 5
      5 3
      5 1
      0 0))
  (testing "limited available-quantity rejects submission when not enough is available"
    (are [available qty] (not (can-submit? available qty))
      0 1
      3 5
      0 100)))

(ns pigeon-scoops-backend.util-api.integration-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [pigeon-scoops-backend.test-system :as ts]))

(use-fixtures :once ts/system-fixture (ts/make-account-fixture))

(deftest constants-test
  (testing "returns constants without auth"
    (let [{:keys [status body]} (ts/test-endpoint :get "/v1/constants" {:use-auth? false})]
      (is (= 200 status))
      (is (contains? body :constants/unit-types))
      (is (contains? body :constants/departments))
      (is (contains? body :constants/roles))
      (is (contains? body :constants/order-statuses))
      (is (contains? body :constants/menu-durations))
      (is (seq (:constants/unit-types body)))
      (is (seq (:constants/departments body)))
      (is (seq (:constants/roles body)))
      (is (seq (:constants/order-statuses body)))
      (is (seq (:constants/menu-durations body)))))
  (testing "returns constants with auth"
    (let [{:keys [status]} (ts/test-endpoint :get "/v1/constants" {:use-auth? true})]
      (is (= 200 status)))))

(ns pigeon-scoops-backend.acounts-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.test-system :as ts]))

(use-fixtures :once ts/system-fixture ts/account-fixture ts/token-fixture)

(deftest account-tests
  (testing "Create user account"
    (let [{:keys [status]} (ts/test-endpoint :post "/v1/account" {:auth true})]
      (is (= status 201))))
  (testing "Update user role"
    (let [{:keys [status]} (ts/test-endpoint :put "/v1/account" {:auth true})]
      (is (= status 204))))
  (testing "Delete user account"
    (let [{:keys [status]} (ts/test-endpoint :delete "/v1/account" {:auth true})]
      (is (= status 204)))))

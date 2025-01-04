(ns pigeon-scoops-backend.groceries-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.test-system :as ts]))

(use-fixtures :once ts/system-fixture (ts/make-account-fixture) (ts/make-roles-fixture :manage-groceries))

(def grocery
  {:name "orange" :department :department/produce})

(def updated-grocery
  (assoc grocery :name "lime"))

(def grocery-unit
  {:source         "store"
   :unit-cost      3.3
   :unit-mass      420
   :unit-mass-type :mass/kg})

(def updated-grocery-unit
  (assoc grocery-unit
    :unit-common-type :common/unit
    :unit-common 1000))

(deftest groceries-list-test
  (testing "List groceries"
    (let [{:keys [status body]} (ts/test-endpoint :get "/v1/groceries" {:auth true})]
      (is (= 200 status))
      (is (vector? body)))))

(deftest groceries-crud-test
  (let [grocery-id (atom nil)
        grocery-unit-id (atom nil)]
    (testing "create grocery"
      (let [{:keys [status body]} (ts/test-endpoint :post "/v1/groceries" {:auth true :body grocery})]
        (reset! grocery-id (:grocery-id body))
        (is (= status 201))))
    (testing "update grocery"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/groceries/" @grocery-id) {:auth true :body updated-grocery})]
        (is (= status 204))))
    (testing "create grocery-unit"
      (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/groceries/" @grocery-id "/units") {:auth true :body grocery-unit})]
        (reset! grocery-unit-id (:grocery-unit-id body))
        (is (= status 201))))
    (testing "update grocery-unit"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/groceries/" @grocery-id "/units") {:auth true :body (assoc updated-grocery-unit :id @grocery-unit-id)})]
        (is (= status 204))))
    (testing "delete grocery-unit"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/groceries/" @grocery-id "/units") {:auth true :body {:grocery-unit-id @grocery-unit-id}})]
        (is (= status 204))))
    (testing "delete grocery"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/groceries/" @grocery-id) {:auth true})]
        (is (= status 204))))))

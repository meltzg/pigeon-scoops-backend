(ns pigeon-scoops-backend.user-orders-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.test-system :as ts]))

(use-fixtures :once ts/system-fixture (ts/make-account-fixture) (ts/make-roles-fixture :manage-recipes :manage-orders))

(def order
  {:note "my order"})

(def updated-order
  (assoc order :status :status/complete))

(def order-item
  {:amount      1
   :amount-unit :volume/floz})

(def updated-order-item
  (assoc order-item :status :status/complete))

(def recipe
  {:name         "a spicy meatball"
   :amount       3
   :amount-unit  :mass/lb
   :source       "the book"
   :instructions ["make them"]})

(deftest orders-list-test
  (testing "List orders"
    (let [{:keys [status body]} (ts/test-endpoint :get "/v1/orders" {:auth true})]
      (is (= 200 status))
      (is (vector? body)))))

(deftest orders-crud-test
  (let [order-id (atom nil)
        order-item-id (atom nil)
        {:keys [body]} (ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe})
        recipe-id (:id body)]
    (testing "create order"
      (let [{:keys [status body]} (ts/test-endpoint :post "/v1/orders" {:auth true :body order})]
        (reset! order-id (:id body))
        (is (= status 201))))
    (testing "update order"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/orders/" @order-id) {:auth true :body updated-order})]
        (is (= status 204))))
    (testing "create order-item"
      (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/orders/" @order-id "/items")
                                                    {:auth true :body (assoc order-item :recipe-id recipe-id)})]
        (reset! order-item-id (:id body))
        (is (= status 201))))
    (testing "update order-item"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/orders/" @order-id "/items")
                                               {:auth true :body (assoc updated-order-item
                                                                   :id @order-item-id
                                                                   :recipe-id recipe-id)})]
        (is (= status 204))))
    (testing "delete order-item"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/orders/" @order-id "/items") {:auth true :body {:id @order-item-id}})]
        (is (= status 204))))
    (testing "delete order"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/orders/" @order-id) {:auth true})]
        (is (= status 204))))))

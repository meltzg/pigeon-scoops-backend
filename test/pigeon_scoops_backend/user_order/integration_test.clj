(ns pigeon-scoops-backend.user-order.integration-test
  (:require [clojure.test :refer :all]
            [integrant.repl.state :as state]
            [pigeon-scoops-backend.test-system :as ts]
            [pigeon-scoops-backend.user-order.db :as order-db]
            [pigeon-scoops-backend.user-order.responses :refer [status terminal?]])
  (:import (java.util UUID)))

(use-fixtures :once ts/system-fixture (ts/make-account-fixture) (ts/make-roles-fixture :manage-recipes :manage-orders :manage-menus))

(def order
  {:note "my order"})

(def updated-order
  (assoc order :status :status/complete))

(def order-item
  {:amount      1
   :amount-unit :volume/qt})

(def updated-order-item
  (assoc order-item :status :status/complete))

(def recipe
  {:name         "a spicy meatball"
   :amount       3
   :amount-unit  :mass/lb
   :source       "the book"
   :instructions ["make them"]})

(def menu
  {:name          "test menu"
   :repeats       true
   :active        true
   :duration      3
   :duration-type :duration/month})

(deftest orders-list-test
  (testing "List orders"
    (let [{:keys [status body]} (ts/test-endpoint :get "/v1/orders" {:auth true})]
      (is (= 200 status))
      (is (vector? body)))))

(deftest orders-crud-test
  (let [order-id (atom nil)
        order-item-id (atom nil)
        recipe-id (get-in (ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe}) [:body :id])
        other-recipe-id (get-in (ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe}) [:body :id])
        ;; Create an active menu and add the recipe to it
        menu-id (get-in (ts/test-endpoint :post "/v1/menus" {:auth true :body menu}) [:body :id])
        menu-item-id (get-in (ts/test-endpoint :post (str "/v1/menus/" menu-id "/items")
                                               {:auth true :body {:recipe-id recipe-id}})
                             [:body :id])
        _ (ts/test-endpoint :post (str "/v1/menus/" menu-id "/sizes")
                            {:auth true :body {:menu-item-id menu-item-id
                                               :amount       1
                                               :amount-unit  :volume/pt}})]
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
    (testing "recipe owner can create order-item for recipe not in an active menu"
      (let [{:keys [status]} (ts/test-endpoint :post (str "/v1/orders/" @order-id "/items")
                                               {:auth true :body (assoc order-item :recipe-id other-recipe-id)})]
        (is (= status 201))))
    (testing "recipe owner can create order-item for an invalid size"
      (let [{:keys [status]} (ts/test-endpoint :post (str "/v1/orders/" @order-id "/items")
                                               {:auth true :body (assoc order-item :recipe-id recipe-id :amount-unit :volume/c)})]
        (is (= status 201))))
    (testing "update order-item"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/orders/" @order-id "/items")
                                               {:auth true :body (assoc updated-order-item
                                                                   :id @order-item-id
                                                                   :recipe-id recipe-id)})]
        (is (= status 204))))
    (testing "recipe owner can update order-item for recipe not in an active menu"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/orders/" @order-id "/items")
                                               {:auth true :body (assoc order-item
                                                                   :id @order-item-id
                                                                   :recipe-id other-recipe-id
                                                                   :status :status/in-progress)})]
        (is (= status 204))))
    (testing "recipe owner can update order-item for an invalid size"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/orders/" @order-id "/items")
                                               {:auth true :body (assoc order-item
                                                                   :id @order-item-id
                                                                   :recipe-id recipe-id
                                                                   :status :status/in-progress
                                                                   :amount-unit :volume/c)})]
        (is (= status 204))))
    (let [{:keys [body]} (ts/test-endpoint :post "/v1/orders" {:auth true :use-other-user true :body order})
          order-id (:id body)
          order-item-id (atom nil)]
      (testing "other user can create order item for active recipe"
        (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/orders/" order-id "/items")
                                                      {:auth true :use-other-user true :body (assoc order-item :recipe-id recipe-id)})]
          (reset! order-item-id (:id body))
          (is (= status 201))))
      (testing "other user cannot create order-item for recipe not in an active menu"
        (let [{:keys [status]} (ts/test-endpoint :post (str "/v1/orders/" order-id "/items")
                                                 {:auth           true
                                                  :use-other-user true
                                                  :body           (assoc order-item :recipe-id other-recipe-id)})]
          (is (= status 400))))
      (testing "other user cannot create order-item for an invalid size"
        (let [{:keys [status]} (ts/test-endpoint :post (str "/v1/orders/" order-id "/items")
                                                 {:auth           true
                                                  :use-other-user true
                                                  :body           (assoc order-item :recipe-id recipe-id :amount-unit :volume/c)})]
          (is (= status 400))))
      (testing "other user cannot update order-item for recipe not in an active menu"
        (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/orders/" order-id "/items")
                                                 {:auth true :use-other-user true :body (assoc order-item
                                                                                          :id @order-item-id
                                                                                          :status :status/in-progress
                                                                                          :recipe-id other-recipe-id)})]
          (is (= status 400))))
      (testing "other user cannot update order-item for an invalid size"
        (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/orders/" order-id "/items")
                                                 {:auth true :use-other-user true :body (assoc order-item
                                                                                          :id @order-item-id
                                                                                          :status :status/in-progress
                                                                                          :recipe-id recipe-id
                                                                                          :amount-unit :volume/c)})]
          (is (= status 400)))))
    (testing "retrieve order bom"
      (let [{:keys [status]} (ts/test-endpoint :get (str "/v1/orders/" @order-id "/bom")
                                               {:auth true})]
        (is (= status 200))))
    (testing "can only delete order item that are not in a terminal state"
      (mapv #(let [order-item-id (-> (ts/test-endpoint :post (str "/v1/orders/" @order-id "/items")
                                                       {:auth true :body (assoc order-item :recipe-id recipe-id)})
                                     :body
                                     :id)
                   _ (ts/test-endpoint :put (str "/v1/orders/" @order-id "/items")
                                       {:auth true :body (assoc order-item
                                                           :recipe-id recipe-id
                                                           :id order-item-id
                                                           :status %)})
                   {:keys [body status]} (ts/test-endpoint :delete (str "/v1/orders/" @order-id "/items") {:auth true :body {:id order-item-id}})]
               (if-not (terminal? %)
                 (is (= status 204))
                 (is (= status 400))))
            status))
    (testing "delete order"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/orders/" @order-id) {:auth true})]
        (is (= status 204))))))

(deftest accept-orders!-test
  (let [db (:db/postgres state/system)
        recipe-ids (doall (repeatedly 3 #(UUID/fromString (get-in (ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe}) [:body :id]))))
        ;; Create an active menu and add the recipe to it
        menu-id (get-in (ts/test-endpoint :post "/v1/menus" {:auth true :body menu}) [:body :id])
        menu-item-ids (mapv #(get-in (ts/test-endpoint :post (str "/v1/menus/" menu-id "/items")
                                                       {:auth true :body {:recipe-id %}})
                                     [:body :id])
                            recipe-ids)
        _ (mapv #(ts/test-endpoint :post (str "/v1/menus/" menu-id "/sizes")
                                   {:auth true :body {:menu-item-id %
                                                      :amount       1
                                                      :amount-unit  :volume/pt}})
                menu-item-ids)
        order-id (UUID/fromString (get-in (ts/test-endpoint :post "/v1/orders" {:auth true :body order}) [:body :id]))
        _ (mapv #(get-in (ts/test-endpoint :post (str "/v1/orders/" order-id "/items")
                                           {:auth true :use-other-user true :body (assoc order-item :recipe-id %)})
                         [:body :id])
                recipe-ids)]
    (testing "unsubmitted orders are not moved to in-progress"
      (order-db/accept-orders! db (first recipe-ids))
      (is (every? #(= (:order-item/status %) :status/draft)
                  (order-db/find-all-order-items db order-id))))
    (testing "submitted orders are moved to in-progress"
      (ts/test-endpoint :put (str "/v1/orders/" order-id)
                        {:auth true :use-other-user true :body {:status :status/submitted :note "foo"}})
      (order-db/accept-orders! db (first recipe-ids))
      (let [items (partition-by #(= (:order-item/recipe-id) (first recipe-ids))
                                (order-db/find-all-order-items db order-id))]
        (is (every? #(= (:order-item/status %) :status/in-progress)
                    (get items true)))
        (is (every? #(= (:order-item/status %) :status/draft)
                    (get items false)))))
    (testing "completed orders are not moved to in-progress"
      (ts/test-endpoint :put (str "/v1/orders/" order-id)
                        {:auth true :use-other-user true :body {:status :status/complete :note "foo"}})
      (order-db/accept-orders! db (first recipe-ids))
      (let [items (partition-by #(= (:order-item/recipe-id) (first recipe-ids))
                                (order-db/find-all-order-items db order-id))]
        (is (every? #(= (:order-item/status %) :status/complete)
                    (get items true)))
        (is (every? #(= (:order-item/status %) :status/draft)
                    (get items false)))))))




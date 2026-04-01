(ns pigeon-scoops-backend.user-order.integration-test
  (:require [clojure.test :refer [are deftest is testing use-fixtures]]
            [integrant.repl.state :as state]
            [pigeon-scoops-backend.test-system :as ts]
            [pigeon-scoops-backend.user-order.db :as order-db]
            [pigeon-scoops-backend.user-order.responses :refer [status terminal?]])
  (:import (java.util UUID)))

(use-fixtures :once ts/system-fixture (ts/make-account-fixture) (ts/make-roles-fixture [:manage-recipes :manage-orders :manage-menus :manage-production] [:manage-recipes :manage-orders :manage-menus]))

(def order
  {:user-order/note "my order"})

(def updated-order
  (assoc order :user-order/note "still my order"))

(def order-item
  {:order-item/amount      1
   :order-item/amount-unit :volume/qt})

(def updated-order-item
  (assoc order-item :order-item/amount 3))

(def recipe
  {:recipe/name         "a spicy meatball"
   :recipe/amount       3
   :recipe/amount-unit  :mass/lb
   :recipe/source       "the book"
   :recipe/instructions ["make them"]})

(def menu
  {:menu/name          "test menu"
   :menu/repeats       true
   :menu/active        true
   :menu/duration      3
   :menu/duration-type :duration/month})

(deftest orders-list-test
  (let [admin-order-id (-> (ts/test-endpoint :post "/v1/orders" {:use-auth? true :body order})
                           :body
                           :id)
        other-order-id (-> (ts/test-endpoint :post "/v1/orders" {:use-auth? true :body order :use-other-user true})
                           :body
                           :id)]
    (testing "List orders"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/orders" {:use-auth? true})]
        (is (= 200 status))
        (is (vector? body))
        (is ((set (map :user-order/id body)) admin-order-id))
        (is (not ((set (map :user-order/id body)) other-order-id)))))
    (testing "List orders. admins can get orders from all users"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/orders" {:use-auth? true :params {:admin true}})]
        (is (= 200 status))
        (is (vector? body))
        (is ((set (map :user-order/id body)) admin-order-id))
        (is ((set (map :user-order/id body)) other-order-id))))
    (testing "List orders. non admins cannot get orders from all users"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/orders" {:use-auth? true :use-other-user true :params {:admin true}})]
        (is (= 200 status))
        (is (vector? body))
        (is (not ((set (map :user-order/id body)) admin-order-id)))
        (is ((set (map :user-order/id body)) other-order-id))))))

(deftest orders-crud-test
  (let [order-id (atom nil)
        order-item-id (atom nil)
        recipe-id (get-in (ts/test-endpoint :post "/v1/recipes" {:use-auth? true :body recipe}) [:body :id])
        other-recipe-id (get-in (ts/test-endpoint :post "/v1/recipes" {:use-auth? true :body recipe}) [:body :id])
        ;; Create an active menu and add the recipe to it
        menu-id (get-in (ts/test-endpoint :post "/v1/menus" {:use-auth? true :body menu}) [:body :id])
        menu-item-id (get-in (ts/test-endpoint :post (str "/v1/menus/" menu-id "/items")
                                               {:use-auth? true :body {:menu-item/recipe-id recipe-id}})
                             [:body :id])
        _ (ts/test-endpoint :post (str "/v1/menus/" menu-id "/items/" menu-item-id "/sizes")
                            {:use-auth? true :body {:menu-item-size/menu-item-id menu-item-id
                                                    :menu-item-size/amount       1
                                                    :menu-item-size/amount-unit  :volume/pt}})]
    (testing "create order"
      (let [{:keys [status body]} (ts/test-endpoint :post "/v1/orders" {:use-auth? true :body order})]
        (reset! order-id (:id body))
        (is (= status 201))))
    (testing "update order"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/orders/" @order-id) {:use-auth? true :body updated-order})]
        (is (= status 204))))
    (testing "create order-item"
      (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/orders/" @order-id "/items")
                                                    {:use-auth? true :body (assoc order-item :order-item/recipe-id recipe-id)})]
        (reset! order-item-id (:id body))
        (is (= status 201))))
    (testing "production manager can create order-item for recipe not in an active menu"
      (let [{:keys [status]} (ts/test-endpoint :post (str "/v1/orders/" @order-id "/items")
                                               {:use-auth? true :body (assoc order-item :order-item/recipe-id other-recipe-id)})]
        (is (= status 201))))
    (testing "production manager can create order-item for an invalid size"
      (let [{:keys [status]} (ts/test-endpoint :post (str "/v1/orders/" @order-id "/items")
                                               {:use-auth? true :body (assoc order-item :order-item/recipe-id recipe-id :order-item/amount-unit :volume/c)})]
        (is (= status 201))))
    (testing "update order-item"
      (let [{:keys [status]} (ts/test-endpoint :patch (str "/v1/orders/" @order-id "/items/" @order-item-id)
                                               {:use-auth? true :body (assoc updated-order-item
                                                                             :order-item/recipe-id recipe-id)})]
        (is (= status 204))))
    (testing "production manager can update order-item for recipe not in an active menu"
      (let [{:keys [status]} (ts/test-endpoint :patch (str "/v1/orders/" @order-id "/items/" @order-item-id)
                                               {:use-auth? true :body (assoc order-item
                                                                             :order-item/recipe-id other-recipe-id)})]
        (is (= status 204))))
    (testing "production manager can update order-item for an invalid size"
      (let [{:keys [status]} (ts/test-endpoint :patch (str "/v1/orders/" @order-id "/items/" @order-item-id)
                                               {:use-auth? true :body (assoc order-item
                                                                             :order-item/recipe-id recipe-id
                                                                             :order-item/amount-unit :volume/c)})]
        (is (= status 204))))
    (testing "update order-item status"
      (let [{:keys [status]} (ts/test-endpoint :patch (str "/v1/orders/" @order-id "/items/" @order-item-id "/status")
                                               {:use-auth? true :body {:order-item/status :status/complete}})]
        (is (= status 204))))
    (let [{:keys [body]} (ts/test-endpoint :post "/v1/orders" {:use-auth? true :use-other-user true :body order})
          order-id (:id body)
          order-item-id (atom nil)]
      (testing "other user can create order item for active recipe"
        (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/orders/" order-id "/items")
                                                      {:use-auth? true :use-other-user true :body (assoc order-item :order-item/recipe-id recipe-id)})]
          (reset! order-item-id (:id body))
          (is (= status 201) (str body))))
      (testing "other user cannot create order-item for recipe not in an active menu"
        (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/orders/" order-id "/items")
                                                      {:use-auth?           true
                                                       :use-other-user true
                                                       :body           (assoc order-item :order-item/recipe-id other-recipe-id)})]
          (is (= status 400) (str body))))
      (testing "other user cannot create order-item for an invalid size"
        (let [{:keys [status]} (ts/test-endpoint :post (str "/v1/orders/" order-id "/items")
                                                 {:use-auth?           true
                                                  :use-other-user true
                                                  :body           (assoc order-item :order-item/recipe-id recipe-id :order-item/amount-unit :volume/c)})]
          (is (= status 400))))
      (testing "other user cannot update order-item for recipe not in an active menu"
        (let [{:keys [status]} (ts/test-endpoint :patch (str "/v1/orders/" order-id "/items/" @order-item-id)
                                                 {:use-auth? true :use-other-user true :body (assoc order-item
                                                                                                    :order-item/recipe-id other-recipe-id)})]
          (is (= status 400))))
      (testing "other user cannot update order-item for an invalid size"
        (let [{:keys [status]} (ts/test-endpoint :patch (str "/v1/orders/" order-id "/items/" @order-item-id)
                                                 {:use-auth? true :use-other-user true :body (assoc order-item
                                                                                                    :order-item/recipe-id recipe-id
                                                                                                    :order-item/amount-unit :volume/c)})]
          (is (= status 400))))
      (testing "non production manager can't move items into in progress/completed"
        (are [status]
             (= (:status (ts/test-endpoint :patch (str "/v1/orders/" order-id "/items/" @order-item-id "/status")
                                           {:use-auth? true :use-other-user true :body {:order-item/status status}}))
                401)
          :status/in-progress
          :status/complete))
      (testing "non production manager can't change the status of items that are in progress, completed or canceled"
        (are [status]
             (do
               (ts/test-endpoint :patch (str "/v1/orders/" order-id "/items/" @order-item-id "/status")
                                 {:use-auth? true :body {:order-item/status status}})
               (= (:status (ts/test-endpoint :patch (str "/v1/orders/" order-id "/items/" @order-item-id "/status")
                                             {:use-auth? true :use-other-user true :body {:order-item/status :status/draft}}))
                  401))
          :status/canceled
          :status/complete
          :status/in-progress))
      (ts/test-endpoint :patch (str "/v1/orders/" order-id "/items/" @order-item-id "/status")
                        {:use-auth? true :body {:order-item/status :status/draft}})
      (testing "non production manager can change the status of items that are not in progress, completed or canceled"
        (are [status]
             (= (:status (ts/test-endpoint :patch (str "/v1/orders/" order-id "/items/" @order-item-id "/status")
                                           {:use-auth? true :use-other-user true :body {:order-item/status status}}))
                204)
          :status/submitted
          :status/draft
          :status/canceled)))
    (testing "retrieve order bom"
      (let [{:keys [status]} (ts/test-endpoint :get (str "/v1/orders/" @order-id "/bom")
                                               {:use-auth? true})]
        (is (= status 200))))
    (testing "can only delete order item that are not in a terminal state"
      (mapv #(let [order-item-id (-> (ts/test-endpoint :post (str "/v1/orders/" @order-id "/items")
                                                       {:use-auth? true :body (assoc order-item :order-item/recipe-id recipe-id)})
                                     :body
                                     :id)
                   _ (ts/test-endpoint :patch (str "/v1/orders/" @order-id "/items/" order-item-id "/status")
                                       {:use-auth? true :body {:order-item/status %}})
                   {:keys [status]} (ts/test-endpoint :delete (str "/v1/orders/" @order-id "/items/" order-item-id)
                                                      {:use-auth? true :body {:order-item/id order-item-id}})]
               (if-not (terminal? %)
                 (is (= status 204))
                 (is (= status 400))))
            status))))

(deftest bulk-status-update-test
  (let [db (:db/postgres state/system)
        recipe-ids (doall (repeatedly 3 #(UUID/fromString (get-in (ts/test-endpoint :post "/v1/recipes" {:use-auth? true :body recipe}) [:body :id]))))
        ;; Create an active menu and add the recipe to it
        menu-id (get-in (ts/test-endpoint :post "/v1/menus" {:use-auth? true :body menu}) [:body :id])
        menu-item-ids (mapv #(get-in (ts/test-endpoint :post (str "/v1/menus/" menu-id "/items")
                                                       {:use-auth? true :body {:menu-item/recipe-id %}})
                                     [:body :id])
                            recipe-ids)
        _ (mapv #(ts/test-endpoint :post (str "/v1/menus/" menu-id "/sizes")
                                   {:use-auth? true :body {:menu-item-size/menu-item-id %
                                                           :menu-item-size/amount       1
                                                           :menu-item-size/amount-unit  :volume/pt}})
                menu-item-ids)
        order-id (UUID/fromString (get-in (ts/test-endpoint :post "/v1/orders" {:use-auth? true :use-other-user true :body order}) [:body :id]))
        order-item-map (into {} (map #(let [order-item-body (assoc order-item :order-item/recipe-id %)]
                                        [(get-in (ts/test-endpoint :post (str "/v1/orders/" order-id "/items")
                                                                   {:use-auth? true :use-other-user true :body order-item-body})
                                                 [:body :id])
                                         order-item-body])
                                     recipe-ids))]
    (testing "unsubmitted orders are canceled"
      (order-db/bulk-status-update! db {:status/submitted :status/in-progress
                                        :status/draft :status/canceled}
                                    (first recipe-ids))
      (let [items (group-by #(= (:order-item/recipe-id %) (first recipe-ids))
                            (order-db/find-all-order-items db order-id))]
        (is (every? #(= (:order-item/status %) :status/canceled)
                    (get items true)))
        (is (every? #(= (:order-item/status %) :status/draft)
                    (get items false)))))
    (testing "submitted orders are moved to in-progress"
      (dorun
       (map (fn [[item-id body]]
              (when (= (:order-item/recipe-id body) (first recipe-ids))
                (ts/test-endpoint :patch (str "/v1/orders/" order-id "/items/" item-id "/status")
                                  {:use-auth? true
                                   :use-other-user true
                                   :body {:order-item/status :status/submitted}})))
            order-item-map))
      (order-db/bulk-status-update! db {:status/submitted :status/in-progress
                                        :status/draft :status/canceled}
                                    (first recipe-ids))
      (let [items (group-by #(= (:order-item/recipe-id %) (first recipe-ids))
                            (order-db/find-all-order-items db order-id))]
        (is (every? #(= (:order-item/status %) :status/in-progress)
                    (get items true)))
        (is (every? #(= (:order-item/status %) :status/draft)
                    (get items false)))))
    (testing "completed orders are not moved to in-progress"
      (dorun
       (map (fn [[item-id body]]
              (when (= (:order-item/recipe-id body) (first recipe-ids))
                (ts/test-endpoint :patch (str "/v1/orders/" order-id "/items/" item-id "/status")
                                  {:use-auth? true
                                   :use-other-user true
                                   :body (assoc body
                                                :order-item/status :status/complete)})))
            order-item-map))
      (order-db/bulk-status-update! db {:status/submitted :status/in-progress
                                        :status/draft :status/canceled}
                                    (first recipe-ids))
      (let [items (group-by #(= (:order-item/recipe-id %) (first recipe-ids))
                            (order-db/find-all-order-items db order-id))]
        (is (every? #(= (:order-item/status %) :status/complete)
                    (get items true)))
        (is (every? #(= (:order-item/status %) :status/draft)
                    (get items false)))))))

(deftest list-in-progress-items-test
  (let [db (:db/postgres state/system)
        recipe-ids (doall (repeatedly 2 #(UUID/fromString (get-in (ts/test-endpoint :post "/v1/recipes" {:use-auth? true :body recipe}) [:body :id]))))
        order-id (UUID/fromString (get-in (ts/test-endpoint :post "/v1/orders" {:use-auth? true :body order}) [:body :id]))]
    (mapv #(let [order-item-body (assoc order-item :order-item/recipe-id %)]
             [(get-in (ts/test-endpoint :post (str "/v1/orders/" order-id "/items")
                                        {:use-auth? true :body order-item-body})
                      [:body :id])
              order-item-body])
          (apply concat (repeat 2 recipe-ids)))
    (order-db/bulk-status-update! db {:status/draft :status/in-progress}
                                  (first recipe-ids))
    (testing "in progress items are consolidated and returned"
      (let [consolidated-items (:body (ts/test-endpoint :get "/v1/production" {:use-auth? true}))]
        (is (= (count consolidated-items) 1))
        (is (= (parse-uuid (:order-item/recipe-id (first consolidated-items))) (first recipe-ids)))
        (is (= (:order-item/amount (first consolidated-items))
               (->> order-item
                    :order-item/amount
                    (* 2.0))))))))

(ns pigeon-scoops-backend.menu.integration-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.test-system :as ts]))

(use-fixtures :once ts/system-fixture (ts/make-account-fixture) (ts/make-roles-fixture :manage-recipes :manage-menus))

(def menu
  {:name          "test menu"
   :repeats       true
   :active        false
   :duration      3
   :duration-type :duration/month})

(def recipe
  {:name         "a spicy meatball"
   :amount       3
   :amount-unit  :mass/lb
   :source       "the book"
   :instructions ["make them"]})

(deftest menus-list-test
  (testing "List menus"
    (let [{:keys [status body]} (ts/test-endpoint :get "/v1/menus" {:auth true})]
      (is (= 200 status))
      (is (vector? body)))))

(deftest menus-crud-test
  (let [menu-id (atom nil)
        menu-item-id (atom nil)
        menu-item-size-id (atom nil)
        recipe-ids (map #(get-in % [:body :id]) (repeatedly 2 #(ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe})))]
    (testing "create inactive menu"
      (let [{:keys [status body]} (ts/test-endpoint :post "/v1/menus" {:auth true :body menu})
            {menu-body :body} (ts/test-endpoint :get (str "/v1/menus/" (:id body))
                                                {:auth true})]
        (reset! menu-id (:id body))
        (is (= status 201))
        (is (nil? (:menu/end-time menu-body)))))
    (testing "create active menu"
      (let [{:keys [status body]} (ts/test-endpoint :post "/v1/menus" {:auth true :body (assoc menu :active true)})
            {menu-body :body} (ts/test-endpoint :get (str "/v1/menus/" (:id body))
                                                {:auth true})]
        (is (= status 201))
        (is (some? (:menu/end-time menu-body)))))
    (let [original-endtime (atom nil)]
      (testing "activate menu populates end time"
        (let [{:keys [status]} (ts/test-endpoint
                                 :put
                                 (str "/v1/menus/" @menu-id)
                                 {:auth true :body (assoc menu :active true)})
              {menu-body :body} (ts/test-endpoint :get (str "/v1/menus/" @menu-id)
                                                  {:auth true})]
          (is (= status 204))
          (is (some? (:menu/end-time menu-body)))
          (reset! original-endtime (:menu/end-time menu-body))))
      (testing "update menu"
        (let [{:keys [status]} (ts/test-endpoint
                                 :put
                                 (str "/v1/menus/" @menu-id)
                                 {:auth true :body (assoc menu :duration 4 :active true)})
              {menu-body :body} (ts/test-endpoint :get (str "/v1/menus/" @menu-id)
                                                  {:auth true})]
          (is (= status 204))
          (is (= (:menu/end-time menu-body) @original-endtime)))))
    (testing "deactivate menu nils end time"
      (let [{:keys [status]} (ts/test-endpoint
                               :put
                               (str "/v1/menus/" @menu-id)
                               {:auth true :body (assoc menu :active false)})
            {menu-body :body} (ts/test-endpoint :get (str "/v1/menus/" @menu-id)
                                                {:auth true})]
        (is (= status 204))
        (is (nil? (:menu/end-time menu-body)))))
    (testing "create menu-item"
      (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/menus/" @menu-id "/items")
                                                    {:auth true :body {:recipe-id (first recipe-ids)}})]
        (reset! menu-item-id (:id body))
        (is (= status 201))))
    (testing "update menu-item"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/menus/" @menu-id "/items")
                                               {:auth true :body {:id        @menu-item-id
                                                                  :recipe-id (second recipe-ids)}})]
        (is (= status 204))))
    (testing "create menu item size"
      (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/menus/" @menu-id "/sizes")
                                                    {:auth true :body {:menu-item-id @menu-item-id
                                                                       :amount       4
                                                                       :amount-unit  :mass/lb}})]
        (reset! menu-item-size-id (:id body))
        (is (= status 201))))
    (testing "update menu item size"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/menus/" @menu-id "/sizes")
                                               {:auth true :body {:id           @menu-item-size-id
                                                                  :menu-item-id @menu-item-id
                                                                  :amount       3
                                                                  :amount-unit  :volume/gal}})]
        (is (= status 204))))
    (testing "retrieve full menu"
      (let [{:keys [status]} (ts/test-endpoint :get (str "/v1/menus/" @menu-id)
                                               {:auth true})]
        (is (= status 200))))
    (let [{:keys [body]} (ts/test-endpoint :post "/v1/menus" {:auth true :body menu})
          other-menu-id (:id body)]
      (testing "cannot update menu item without matching menu id"
        (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/menus/" other-menu-id "/items")
                                                 {:auth true :body {:id        @menu-item-id
                                                                    :recipe-id (second recipe-ids)}})]
          (is (= status 400))))
      (testing "cannot update menu item size without matching menu id"
        (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/menus/" other-menu-id "/sizes")
                                                 {:auth true :body {:id           @menu-item-size-id
                                                                    :menu-item-id @menu-item-id
                                                                    :amount       3
                                                                    :amount-unit  :volume/gal}})]
          (is (= status 400))))
      (testing "cannot delete menu item size without matching menu id"
        (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/menus/" other-menu-id "/sizes") {:auth true
                                                                                                    :body {:id @menu-item-size-id}})]
          (is (= status 400))))
      (testing "cannot delete menu item without matching menu id"
        (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/menus/" other-menu-id "/items") {:auth true
                                                                                                    :body {:id @menu-item-id}})]
          (is (= status 400)))))
    (testing "delete menu item size"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/menus/" @menu-id "/sizes") {:auth true
                                                                                             :body {:id @menu-item-size-id}})]
        (is (= status 204))))
    (testing "delete menu item"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/menus/" @menu-id "/items") {:auth true
                                                                                             :body {:id @menu-item-id}})]
        (is (= status 204))))
    (testing "delete menu"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/menus/" @menu-id) {:auth true})]
        (is (= status 204))))))

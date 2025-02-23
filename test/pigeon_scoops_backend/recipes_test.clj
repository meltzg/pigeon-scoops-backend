(ns pigeon-scoops-backend.recipes-test
  (:require [clojure.test :refer :all]
            [integrant.repl.state]
            [pigeon-scoops-backend.server :refer :all]
            [pigeon-scoops-backend.test-system :as ts]))

(use-fixtures
  :once
  ts/system-fixture
  (ts/make-account-fixture)
  (ts/make-roles-fixture :manage-recipes :manage-groceries))

(def grocery
  {:name "orange" :department :department/produce})

(def recipe
  {:name         "a spicy meatball"
   :amount       3
   :amount-unit  :mass/lb
   :source       "the book"
   :instructions ["make them"]})

(def updated-recipe
  (assoc recipe :public true))

(def ingredient
  {:amount      1
   :amount-unit :volume/floz})

(deftest recipes-list-test
  (testing "List recipes"
    (ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe})
    (testing "with auth -- public and private"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/recipes" {:auth true})]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (vector? (:private body)))))
    (testing "without auth -- public"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/recipes" {:auth false})]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (nil? (:private body)))))))

(deftest recipes-crud-test
  (let [recipe-id (atom nil)
        ingredient-id (atom nil)
        {:keys [body]} (ts/test-endpoint :post "/v1/groceries" {:auth true :body grocery})
        grocery-id (:id body)]
    (testing "create recipe"
      (let [{:keys [status body]} (ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe})]
        (reset! recipe-id (:id body))
        (is (= status 201))))
    (testing "update recipe"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/recipes/" @recipe-id) {:auth true :body updated-recipe})]
        (is (= status 204))))
    (testing "favorite recipe"
      (let [{:keys [status]} (ts/test-endpoint :post (str "/v1/recipes/" @recipe-id "/favorite") {:auth true})]
        (is (= status 204))))
    (testing "unfavorite recipe"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/recipes/" @recipe-id "/favorite") {:auth true})]
        (is (= status 204))))
    (testing "create ingredient from recipe"
      (let [ingredient (assoc ingredient :ingredient-recipe-id @recipe-id)
            {:keys [status body]} (ts/test-endpoint :post (str "/v1/recipes/" @recipe-id "/ingredients") {:auth true :body ingredient})]
        (reset! ingredient-id (:id body))
        (is (= status 201))))
    (testing "update ingredient"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/recipes/" @recipe-id "/ingredients") {:auth true :body (assoc ingredient
                                                                                                                       :id @ingredient-id
                                                                                                                       :amount 3000
                                                                                                                       :ingredient-recipe-id @recipe-id)})]
        (is (= status 204))))
    (testing "switching from recipe to grocery ingredient"
      (let [ingredient (-> ingredient
                           (assoc :ingredient-grocery-id grocery-id)
                           (dissoc :ingredient-recipe-id))
            {:keys [status]} (ts/test-endpoint :put (str "/v1/recipes/" @recipe-id "/ingredients") {:auth true :body (assoc ingredient :id @ingredient-id)})]
        (is (= status 204))))
    (testing "create ingredient from grocery"
      (let [ingredient (assoc ingredient :ingredient-grocery-id grocery-id)
            {:keys [status]} (ts/test-endpoint :post (str "/v1/recipes/" @recipe-id "/ingredients") {:auth true :body ingredient})]
        (is (= status 201))))
    (testing "retrieve recipe"
      (let [{:keys [status]} (ts/test-endpoint :get (str "/v1/recipes/" @recipe-id) {:auth true})]
        (is (= status 200))))
    (testing "delete ingredient"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/recipes/" @recipe-id "/ingredients") {:auth true :body {:id @ingredient-id}})]
        (is (= status 204))))
    (testing "delete recipe"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/recipes/" @recipe-id) {:auth true})]
        (is (= status 204))))))

(comment
  (let [auth (:auth/auth0 integrant.repl.state/system)]
    (reset! ts/token (ts/get-test-token (assoc auth :username "repl-user@pigeon-scoops.com"
                                                    :password (:test-password auth))))
    (ts/test-endpoint :post "/v1/account" {:auth true}))
  @ts/test-user
  @ts/token
  recipe
  (let [recipe-id (-> (ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe})
                      :body
                      :recipe-id)]
    (ts/test-endpoint :post (str "/v1/recipes/" recipe-id "/favorite") {:auth true}))
  (ts/test-endpoint :get "/v1/recipes" {:auth true}))

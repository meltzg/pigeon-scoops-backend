(ns pigeon-scoops-backend.recipes-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.server :refer :all]
            [pigeon-scoops-backend.test-system :as ts]))

(use-fixtures :once ts/token-fixture)

(def recipe
  {:img       "http://image.com/foo.png"
   :prep-time 100
   :name      "a spicy meatball"})

(def updated-recipe
  (assoc recipe :public true))

(def step
  {:sort        1
   :description "put the lime in the coconut"})

(def updated-step
  (assoc step :description "mix it all about"))

(deftest recipes-list-test
  (testing "List recipes"
    (testing "with auth -- public and drafts"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/recipes" {:auth true})]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (vector? (:drafts body)))))
    (testing "without auth -- public"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/recipes" {:auth false})]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (nil? (:drafts body)))))))

(deftest recipes-crud-test
  (let [recipe-id (atom nil)
        step-id (atom nil)]
    (testing "create recipe"
      (let [{:keys [status body]} (ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe})]
        (reset! recipe-id (:recipe-id body))
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
    (testing "create step"
      (let [{:keys [status body]} (ts/test-endpoint :post (str "/v1/recipes/" @recipe-id "/step") {:auth true :body step})]
        (reset! step-id (:step-id body))
        (is (= status 201))))
    (testing "update step"
      (let [{:keys [status]} (ts/test-endpoint :put (str "/v1/recipes/" @recipe-id "/step") {:auth true :body (assoc updated-step :step-id @step-id)})]
        (is (= status 204))))
    (testing "delete step"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/recipes/" @recipe-id "/step") {:auth true :body {:step-id @step-id}})]
        (is (= status 204))))
    (testing "delete recipe"
      (let [{:keys [status]} (ts/test-endpoint :delete (str "/v1/recipes/" @recipe-id) {:auth true})]
        (is (= status 204))))))

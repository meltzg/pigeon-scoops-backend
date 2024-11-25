(ns pigeon-scoops-backend.recipes-test
  (:require [clojure.test :refer :all]
            [pigeon-scoops-backend.server :refer :all]
            [pigeon-scoops-backend.test-system :as ts]))

(def recipe
  {:img "http://image.com/foo.png"
   :prep-time 100
   :name "a spicy meatball"})

(deftest recipes-list-test
  (testing "List recipes"
    (testing "with auth -- public and drafts"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/recipes" {:auth true})]
        (println body)
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (vector? (:drafts body)))))
    (testing "without auth -- public"
      (let [{:keys [status body]} (ts/test-endpoint :get "/v1/recipes" {:auth false})]
        (is (= 200 status))
        (is (vector? (:public body)))
        (is (nil? (:drafts body)))))))

(deftest recipes-crud-test
  (testing "create recipe")
  (testing "update recipe")
  (testing "delete recipe"))

(comment
  (ts/test-endpoint :post "/v1/recipes" {:auth true :body recipe}))
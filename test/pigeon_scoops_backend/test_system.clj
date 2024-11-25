(ns pigeon-scoops-backend.test-system
  (:require [clojure.test :refer :all]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [pigeon-scoops-backend.auth0 :as auth0]
            [ring.mock.request :as mock]))

(defn test-endpoint
  ([method uri]
   (test-endpoint method uri nil))
  ([method uri opts]
   (let [app (-> state/system :pigeon-scoops-backend/app)
         response (app (-> (mock/request method uri)
                           (cond-> (:auth opts) (mock/header :authorization (str "Bearer " (auth0/get-test-token)))
                                   (:body opts) (mock/json-body (:body opts)))
                           ))]
     (update response :body (partial m/decode "application/json")))))

(comment
  (test-endpoint :get "/v1/recipes")
  (test-endpoint :post "/v1/recipes" {:body {:img       "string"
                                              :name      "my name"
                                              :prep-time 30}}))

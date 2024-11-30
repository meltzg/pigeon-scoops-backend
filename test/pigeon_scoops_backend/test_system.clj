(ns pigeon-scoops-backend.test-system
  (:require [clojure.test :refer :all]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [pigeon-scoops-backend.auth0 :as auth0]
            [ring.mock.request :as mock]))

(def token (atom nil))

(defn token-fixture [f]
  (reset! token (auth0/get-test-token))
  (f)
  (reset! token nil))

(defn test-endpoint
  ([method uri]
   (test-endpoint method uri nil))
  ([method uri opts]
   (let [app (-> state/system :pigeon-scoops-backend/app)
         response (app (-> (mock/request method uri)
                           (cond-> (:auth opts) (mock/header :authorization (str "Bearer " (or @token (auth0/get-test-token))))
                                   (:body opts) (mock/json-body (:body opts)))))]
     (update response :body (partial m/decode "application/json")))))

(comment
  (test-endpoint :get "/v1/recipes/c4aa000e-24c7-46b0-8a5e-1fcb0a5f8e30")
  (test-endpoint :post "/v1/recipes" {:auth true
                                      :body {:img       "string"
                                             :name      "my name"
                                             :prep-time 30}})
  (test-endpoint :delete "/v1/recipes/4fa73cb5-e0cd-471f-b5c2-b080eb2039ab/favorite" {:auth true})
  (test-endpoint :post "/v1/recipes/c4aa000e-24c7-46b0-8a5e-1fcb0a5f8e30/step"
                 {:auth true
                  :body {:sort        1
                         :description "shake"}}))

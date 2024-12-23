(ns pigeon-scoops-backend.test-system
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [pigeon-scoops-backend.auth0 :as auth0]
            [ring.mock.request :as mock]))

(def token (atom nil))

(defn port-available? [port]
  (try
    (.close (java.net.Socket. "localhost" port))
    false
    (catch Exception _
      true)))

(defn find-next-available-port [ports]
  (first (filter port-available? ports)))

(defn system-fixture [f]
  (let [port (find-next-available-port (range 3000 4000))]
    (cond state/system
          (f)
          port
          (do
            (ig-repl/set-prep!
              (fn []
                (-> "resources/config.edn"
                    slurp
                    ig/read-string
                    ig/expand
                    (assoc-in [:server/jetty :port] port))))
            (ig-repl/go)
            (f)
            (ig-repl/halt))
          :else
          (throw (RuntimeException. "No available port")))))

(defn token-fixture [f]
  (let [auth (-> state/system :auth/auth0)]
    (reset! token (auth0/get-test-token auth))
    (f)
    (reset! token nil)))

(defn test-endpoint
  ([method uri]
   (test-endpoint method uri nil))
  ([method uri opts]
   (let [app (-> state/system :pigeon-scoops-backend/app)
         auth (-> state/system :auth/auth0)
         response (app (-> (mock/request method uri)
                           (cond-> (:auth opts) (mock/header :authorization (str "Bearer " (or @token (auth0/get-test-token auth))))
                                   (:body opts) (mock/json-body (:body opts)))))]
     (update response :body (partial m/decode "application/json")))))

(comment
  (test-endpoint :get "/v1/recipes/c4aa000e-24c7-46b0-8a5e-1fcb0a5f8e30")
  (test-endpoint :post "/v1/recipes" {:auth true
                                      :body {:img       "string"
                                             :name      "my name"
                                             :prep-time 30}})
  (test-endpoint :delete "/v1/recipes/4fa73cb5-e0cd-471f-b5c2-b080eb2039ab/favorite" {:auth true})
  (test-endpoint :post "/v1/recipes/c4aa000e-24c7-46b0-8a5e-1fcb0a5f8e30/steps"
                 {:auth true
                  :body {:sort        1
                         :description "shake"}}))

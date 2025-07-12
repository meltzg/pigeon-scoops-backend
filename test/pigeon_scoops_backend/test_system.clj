(ns pigeon-scoops-backend.test-system
  (:require [clj-http.client :as http]
            [clojure.test :refer :all]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [pigeon-scoops-backend.auth0 :as auth0]
            [pigeon-scoops-backend.config :as config]
            [ring.mock.request :as mock])
  (:import (java.net Socket)
           (java.util UUID)
           (org.testcontainers.containers PostgreSQLContainer)))

(def tokens (atom nil))
(def test-users (atom nil))

(defn get-test-token [{:keys [test-client-id username password]}]
  (->> {:content-type  :json
        :cookie-policy :standard
        :body          (m/encode "application/json"
                                 {:client_id  test-client-id
                                  :audience   "https://pigeon-scoops.us.auth0.com/api/v2/"
                                  :grant_type "password"
                                  :username   username
                                  :password   password
                                  :scope      "openid profile email"})}
       (http/post "https://pigeon-scoops.us.auth0.com/oauth/token")
       (m/decode-response-body)
       :access_token))

(defn test-endpoint
  ([method uri]
   (test-endpoint method uri nil))
  ([method uri opts]
   (let [app (-> state/system :pigeon-scoops-backend/app)
         auth (-> state/system :auth/auth0)
         token (or (if (:use-other-user opts) (second @tokens) (first @tokens))
                   (if (:use-other-user opts) (get-test-token (conj auth (second @test-users)))
                                              (get-test-token (conj auth (first @test-users)))))
         response (app (-> (mock/request method uri (:params opts))
                           (cond-> (:auth opts) (mock/header :authorization (str "Bearer " token))
                                   (:body opts) (mock/json-body (:body opts)))))
         response (update response :body (partial m/decode "application/json"))]
     (println method uri opts response)
     response)))

(defn port-available? [port]
  (try
    (let [^String host "localhost"
          ^Integer port port]
      (.close (Socket. host port)))
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
            (let [postgres-container
                  (doto (PostgreSQLContainer. "postgres:latest") ;; Full reference to PostgreSQLContainer
                    (.withDatabaseName "test_db")
                    (.withUsername "user")
                    (.withPassword "password")
                    (.start))
                  full-uri (str (.getJdbcUrl postgres-container)
                                "&user=" (.getUsername postgres-container)
                                "&password=" (.getPassword postgres-container))]
              (ig-repl/set-prep!
                (fn []
                  (-> (if (env :ci-env)
                        "resources/config.edn"
                        "dev/resources/config.edn")
                      (config/load-config)
                      (assoc-in [:db/postgres :jdbc-url] full-uri)
                      (assoc-in [:server/jetty :port] port)
                      (ig/expand))))
              (ig-repl/go)
              (try
                (f)
                (catch Exception e
                  (println "FATAL" e)))
              (ig-repl/halt)
              (.stop postgres-container)))
          :else
          (throw (RuntimeException. "No available port")))))

(defn make-account-fixture
  ([]
   (make-account-fixture true))
  ([manage-user?]
   (fn [f]
     (let [auth (:auth/auth0 state/system)
           usernames (repeatedly 2 #(str "integration-test" (UUID/randomUUID) "@pigeon-scoops.com"))
           passwords (repeatedly 2 #(str (UUID/randomUUID)))
           create-responses (map #(auth0/create-user! auth
                                                      {:connection "Username-Password-Authentication"
                                                       :email      %1
                                                       :password   %2})
                                 usernames
                                 passwords)]
       (reset! test-users (mapv #(hash-map :username %1
                                           :password %2
                                           :uid (:user_id %3))
                                usernames
                                passwords
                                create-responses))
       (reset! tokens (mapv #(get-test-token (conj auth %)) @test-users))
       (when manage-user?
         (mapv #(test-endpoint :post "/v1/account" {:auth true :use-other-user %}) [true false]))
       (f)
       (when manage-user?
         (mapv #(test-endpoint :delete "/v1/account" {:auth true :use-other-user %}) [true false]))))))

(defn make-roles-fixture [& roles]
  (fn [f]
    (let [auth (:auth/auth0 state/system)]
      (mapv #(auth0/update-roles! auth (:uid %) roles) @test-users)
      (reset! tokens (mapv #(get-test-token (conj auth %)) @test-users))
      (f)
      (reset! tokens nil))))

(ns pigeon-scoops-backend.test-system
  (:require [clj-http.client :as http]
            [clojure.test :refer :all]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [pigeon-scoops-backend.auth0 :as auth0]
            [ring.mock.request :as mock])
  (:import (java.net Socket)
           (java.util UUID)
           (org.testcontainers.containers PostgreSQLContainer)))

(def token (atom nil))
(def test-user (atom nil))

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
         response (app (-> (mock/request method uri (:params opts))
                           (cond-> (:auth opts) (mock/header :authorization (str "Bearer " (or @token (get-test-token (conj auth @test-user)))))
                                   (:body opts) (mock/json-body (:body opts)))))
         response (update response :body (partial m/decode "application/json"))]
     (println method uri opts response)
     response)))

(defn port-available? [port]
  (try
    (.close (Socket. ^String "localhost" ^Integer port))
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
                      slurp
                      ig/read-string
                      (assoc-in [:db/postgres :jdbc-url] full-uri)
                      ig/expand
                      (assoc-in [:server/jetty :port] port))))
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
           username (str "integration-test" (UUID/randomUUID) "@pigeon-scoops.com")
           password (str (UUID/randomUUID))
           create-response (auth0/create-user! auth
                                               {:connection "Username-Password-Authentication"
                                                :email      username
                                                :password   password})]
       (reset! test-user {:username username
                          :password password
                          :uid      (:user_id create-response)})
       (reset! token (get-test-token (conj auth @test-user)))
       (when manage-user?
         (test-endpoint :post "/v1/account" {:auth true}))
       (f)
       (when manage-user?
         (test-endpoint :delete "/v1/account" {:auth true}))))))

(defn make-roles-fixture [& roles]
  (fn [f]
    (let [auth (:auth/auth0 state/system)]
      (auth0/update-roles! auth (:uid @test-user) roles)
      (reset! token (get-test-token (conj auth @test-user)))
      (f)
      (reset! token nil))))

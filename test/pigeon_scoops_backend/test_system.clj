(ns pigeon-scoops-backend.test-system
  (:require [clj-http.client :as http]
            [clojure.test :refer :all]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [pigeon-scoops-backend.auth0 :as auth0]
            [pigeon-scoops-backend.db :as config]
            [pigeon-scoops-backend.db-tasks]
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
   (let [app (-> state/system :server/routes)
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

(defn strict-fixture [{:keys [setup teardown msg]}]
  (fn [f]
    (try
      (when setup (setup))
      (try
        (f)
        (finally
          (when teardown (teardown))))
      (catch Throwable t
        (do-report {:type :error :message msg :expected nil :actual t})
        (throw t)))))


(def system-fixture
  (let [postgres-container (atom nil)]
    (strict-fixture
      {:msg "failed to set up test system"
       :setup (when-not state/system
                (fn []
                  (reset! postgres-container
                          (doto (PostgreSQLContainer. "postgres:latest") ;; Full reference to PostgreSQLContainer
                            (.withDatabaseName "test_db")
                            (.withUsername "user")
                            (.withPassword "password")
                            (.start)))
                  (let [port (find-next-available-port (range 3000 4000))

                        full-uri (str (.getJdbcUrl @postgres-container)
                                      "&user=" (.getUsername @postgres-container)
                                      "&password=" (.getPassword @postgres-container))]
                    (ig-repl/set-prep!
                      (fn []
                        (let [task-system (-> "resources/db-task-config.edn"
                                              (config/load-config)
                                              (assoc-in [:db/postgres :jdbc-url] full-uri)
                                              (config/init-system))
                              migration-task (:db-tasks/migration task-system)]
                          (migration-task)
                          (ig/halt! task-system)
                          (-> (if (env :ci-env)
                                "resources/server-config.edn"
                                "dev/resources/server-config.edn")
                              (config/load-config)
                              (assoc-in [:db/postgres :jdbc-url] full-uri)
                              (assoc-in [:server/jetty :port] port)
                              (ig/expand)))))
                    (ig-repl/go))))
       :teardown (fn []
                   (ig-repl/halt)
                   (.stop @postgres-container))})))

(defn make-account-fixture
  ([]
   (make-account-fixture true))
  ([manage-user?]
   (strict-fixture
     {:setup    (fn []
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
                      (mapv #(test-endpoint :post "/v1/account" {:auth true :use-other-user %}) [true false]))))
      :teardown (fn []
                  (when manage-user?
                    (mapv #(test-endpoint :delete "/v1/account" {:auth true :use-other-user %}) [true false])))
      :msg      "account fixture failed"})))

(defn make-roles-fixture [& roles]
  (strict-fixture
    {:setup    (fn []
                 (let [auth (:auth/auth0 state/system)]
                   (mapv #(auth0/update-roles! auth (:uid %) roles) @test-users)
                   (reset! tokens (mapv #(get-test-token (conj auth %)) @test-users))))
     :teardown #(reset! tokens nil)
     :msg      "make roles failed"}))

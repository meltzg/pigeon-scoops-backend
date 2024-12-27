(ns pigeon-scoops-backend.test-system
  (:require [clj-http.client :as http]
            [clojure.test :refer :all]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [pigeon-scoops-backend.auth0 :as auth0]
            [ring.mock.request :as mock])
  (:import (java.util UUID)))

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
         response (app (-> (mock/request method uri)
                           (cond-> (:auth opts) (mock/header :authorization (str "Bearer " (or @token (get-test-token (conj auth @test-user)))))
                                   (:body opts) (mock/json-body (:body opts)))))]
     (update response :body (partial m/decode "application/json")))))

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
                (-> "dev/resources/config.edn"
                    slurp
                    ig/read-string
                    ig/expand
                    (assoc-in [:server/jetty :port] port))))
            (ig-repl/go)
            (f)
            (ig-repl/halt))
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

(defn recipe-admin-fixture [f]
  (let [auth (:auth/auth0 state/system)]
    (auth0/update-role! auth (:uid @test-user) :manage-recipes)
    (reset! token (get-test-token (conj auth @test-user)))
    (f)))

(defn roles-admin-fixture [f]
  (let [auth (:auth/auth0 state/system)]
    (auth0/update-role! auth (:uid @test-user) :manage-roles)
    (reset! token (get-test-token (conj auth @test-user)))
    (f)))

(defn token-fixture [f]
  (let [auth (-> state/system :auth/auth0)]
    (reset! token (get-test-token (conj auth @test-user)))
    (f)
    (reset! token nil)))

(comment
  (let [auth (:auth/auth0 state/system)
        username "repl-user@pigeon-scoops.com"
        password (:test-password auth)
        create-response (auth0/create-user! auth
                                            {:connection "Username-Password-Authentication"
                                             :email      username
                                             :password   password})]
    (reset! test-user {:username username
                       :password password
                       :uid      (:user_id create-response)})
    (reset! token (get-test-token (conj auth @test-user))))
  (get-test-token (merge (:auth/auth0 state/system) {:username "repl-user@pigeon-scoops.com"
                                                     :password (:test-password (:auth/auth0 state/system))})))

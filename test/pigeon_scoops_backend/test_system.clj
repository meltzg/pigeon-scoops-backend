(ns pigeon-scoops-backend.test-system
  (:require [clj-http.client :as http]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [pigeon-scoops-backend.auth0 :as auth0]
            [ring.mock.request :as mock])
  (:import (java.util UUID)
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
         response (app (-> (mock/request method uri)
                           (cond-> (:auth opts) (mock/header :authorization (str "Bearer " (or @token (get-test-token (conj auth @test-user)))))
                                   (:body opts) (mock/json-body (:body opts)))))
         response (update response :body (partial m/decode "application/json"))]
     (println response)
     response)))

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
                  (println "FATAL")))
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

(comment
  (let [auth (:auth/auth0 state/system)
        username (str "repl-user" (UUID/randomUUID) "@pigeon-scoops.com")
        password (str (UUID/randomUUID))
        create-response (auth0/create-user! auth
                                            {:connection "Username-Password-Authentication"
                                             :email      username
                                             :password   password})]
    (reset! test-user {:username username
                       :password password
                       :uid      (:user_id create-response)})
    (auth0/update-roles! auth
                         (:uid @test-user)
                         [:manage-orders :manage-recipes :manage-groceries])
    (reset! token (get-test-token (conj auth @test-user)))
    (test-endpoint :post "/v1/account" {:auth true})
    (spit "dev/resources/test-user.edn" @test-user))
  (test-endpoint :get "/v1/constants" {:auth true})
  (do
    (->> "dev/resources/test-user.edn"
         (slurp)
         (edn/read-string)
         (reset! test-user)
         (conj (:auth/auth0 state/system))
         (get-test-token)
         (reset! token))
    (test-endpoint :post "/v1/account" {:auth true}))
  (let [grocery-map (->> "/home/meltzg/json_dumps/groceries.json"
                         (slurp)
                         (m/decode "application/json")
                         (map #(vector (:type %) (-> (test-endpoint :post "/v1/groceries"
                                                                    {:auth true
                                                                     :body {:name       (:type %)
                                                                            :department :department/grocery}})
                                                     :body
                                                     :id)))
                         (into {}))
        units (->> "/home/meltzg/json_dumps/grocery_units.json"
                   (slurp)
                   (m/decode "application/json")
                   (map #(-> (test-endpoint :post (str "/v1/groceries/" (get grocery-map (:type %)) "/units")
                                            {:auth true
                                             :body (cond-> {:source    (:source %)
                                                            :unit-cost (:unit_cost %)}
                                                           (:unit_mass %) (merge {:unit-mass      (:unit_mass %)
                                                                                  :unit-mass-type (keyword "mass" (:unit_mass_type %))})
                                                           (:unit_volume %) (merge {:unit-volume      (:unit_volume %)
                                                                                    :unit-volume-type (keyword "volume" (:unit_volume_type %))})
                                                           (:unit_common %) (merge {:unit-common      (:unit_common %)
                                                                                    :unit-common-type (keyword "common" (:unit_common_type %))}))})
                             :body
                             :id)))
        recipe-map (->> "/home/meltzg/json_dumps/recipes.json"
                        (slurp)
                        (m/decode "application/json")
                        (map #(vector (:id %) (-> (test-endpoint :post "/v1/recipes"
                                                                 {:auth true
                                                                  :body (merge (select-keys % [:name :instructions])
                                                                               {:amount      (:amount %)
                                                                                :amount-unit (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                      (:amount_unit %))
                                                                                :source      (or (:source %) "")})})
                                                  :body
                                                  :id)))
                        (into {}))
        ingredients (->> "/home/meltzg/json_dumps/ingredients.json"
                         (slurp)
                         (m/decode "application/json")
                         (map #(-> (test-endpoint :post (str "/v1/recipes/" (get recipe-map (:recipe_id %)) "/ingredients")
                                                  {:auth true
                                                   :body {:ingredient-grocery-id (get grocery-map (:ingredient_type %))
                                                          :amount                (:amount %)
                                                          :amount-unit           (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                          (:amount_unit %))}})
                                   :body
                                   :id)))
        recipe-map (merge recipe-map
                          (->> "/home/meltzg/json_dumps/flavors.json"
                               (slurp)
                               (m/decode "application/json")
                               (map #(let [recipe-id (-> (test-endpoint :post "/v1/recipes"
                                                                        {:auth true
                                                                         :body (merge (select-keys % [:name :instructions])
                                                                                      {:amount      (:amount %)
                                                                                       :amount-unit (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                             (:amount_unit %))
                                                                                       :source      ""})})
                                                         :body
                                                         :id)]
                                       (test-endpoint :post (str "/v1/recipes/" recipe-id "/ingredients")
                                                      {:auth true
                                                       :body {:ingredient-recipe-id (get recipe-map (:recipe_id %))
                                                              :amount               (:amount %)
                                                              :amount-unit          (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                             (:amount_unit %))}})
                                       [(:id %) recipe-id]))
                               (into {})))
        ingredients (concat ingredients
                            (->> "/home/meltzg/json_dumps/mixins.json"
                                 (slurp)
                                 (m/decode "application/json")
                                 (map #(-> (test-endpoint :post (str "/v1/recipes/" (get recipe-map (:flavor_id %)) "/ingredients")
                                                          {:auth true
                                                           :body {:ingredient-recipe-id (get recipe-map (:recipe_id %))
                                                                  :amount               (:amount %)
                                                                  :amount-unit          (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                 (:amount_unit %))}})
                                           :body
                                           :id))))
        order-map (->> "/home/meltzg/json_dumps/orders.json"
                       (slurp)
                       (m/decode "application/json")
                       (map #(vector (:id %) (-> (test-endpoint :post "/v1/orders"
                                                                {:auth true
                                                                 :body (select-keys % [:note])})
                                                 :body
                                                 :id)))
                       (into {}))
        order-items (->> "/home/meltzg/json_dumps/flavor_amounts.json"
                         (slurp)
                         (m/decode "application/json")
                         (map #(-> (test-endpoint :post (str "/v1/orders/" (get order-map (:order_id %)) "/items")
                                                  {:auth true
                                                   :body {:recipe-id   (get recipe-map (:flavor_id %))
                                                          :amount      (:amount %)
                                                          :amount-unit (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                (:amount_unit %))}})
                                   :body
                                   :id)))]
    {:grocery-map   grocery-map
     :grocery-units units
     :recipe-map    recipe-map
     :ingredients   ingredients
     :order-map     order-map
     :order-items   order-items}))
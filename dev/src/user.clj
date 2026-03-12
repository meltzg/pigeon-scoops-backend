(ns user
  (:require [clj-http.client :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [pigeon-scoops-backend.auth :as auth0]
            [pigeon-scoops-backend.db :as db]
            [pigeon-scoops-backend.db-tasks]
            [ring.mock.request :as mock]
            [clojure.pprint :refer [pprint]])
  (:import (java.util UUID)
           (org.testcontainers.containers PostgreSQLContainer)))

(def users-config "dev/resources/repl-users.edn")
(def db-container (atom nil))
(def tokens (atom nil))
(def test-users (atom nil))

(defn get-token [{:keys [test-client-id username password]}]
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

(defn make-request
  ([method uri]
   (make-request method uri nil))
  ([method uri opts]
   (let [app (-> state/system :server/routes)
         auth (-> state/system :auth/auth0)
         response (app (-> (mock/request method uri)
                           (cond-> (:auth opts) (mock/header :authorization (str "Bearer " (or (first @tokens) (get-token (conj auth (first @test-users))))))
                                   (:body opts) (mock/json-body (:body opts)))))
         response (update response :body #(try
                                            (m/decode "application/json" %)
                                            (catch Exception e
                                              (pprint e)
                                              %)))]

     response)))

(defn make-test-users [n]
  (let [auth (:auth/auth0 state/system)
        usernames (repeatedly n #(str "repl-user" (UUID/randomUUID) "@pigeon-scoops.com"))
        passwords (repeatedly n #(str (UUID/randomUUID)))
        create-responses (mapv #(auth0/create-user! auth
                                                    {:connection "Username-Password-Authentication"
                                                     :email      %1
                                                     :password   %2})
                               usernames passwords)]
    (reset! test-users (mapv (fn [u p c]
                               {:username u
                                :password p
                                :uid      (:user_id c)})
                             usernames passwords create-responses))
    (mapv #(auth0/update-roles! auth
                                (:uid %)
                                [:manage-orders :manage-recipes :manage-groceries :manage-menus])
          @test-users)
    (reset! tokens (mapv #(get-token (conj auth %)) @test-users))
    (make-request :post "/v1/account" {:auth true})
    (spit users-config (with-out-str
                         (pprint @test-users)))))

(defn load-test-user []
  (println "loading token")
  (->> users-config
       (slurp)
       (edn/read-string)
       (reset! test-users)
       (first)
       (conj (:auth/auth0 state/system))
       (get-token)
       (vector)
       (reset! tokens))
  (loop [response (make-request :post "/v1/account" {:auth true})
         attempts 0]
    (if (= 401 (:status response))
      (do
        (println "attempt" attempts "failed")
        (Thread/sleep 1000)
        (recur (make-request :post "/v1/account" {:auth true})
               (inc attempts))))))

(defn load-seed-data []
  (let [grocery-map (->> "dev/resources/seed/groceries.json"
                         (slurp)
                         (m/decode "application/json")
                         (map #(vector (:type %) (-> (make-request :post "/v1/groceries"
                                                                   {:auth true
                                                                    :body {:grocery/name       (:type %)
                                                                           :grocery/department :department/grocery}})
                                                     :body
                                                     :id)))
                         (into {}))
        units (->> "dev/resources/seed/grocery_units.json"
                   (slurp)
                   (m/decode "application/json")
                   (map #(-> (make-request :post (str "/v1/groceries/" (get grocery-map (:type %)) "/units")
                                           {:auth true
                                            :body (cond-> {:grocery-unit/source    (:source %)
                                                           :grocery-unit/unit-cost (:unit_cost %)}
                                                    (:unit_mass %) (merge {:grocery-unit/unit-mass      (:unit_mass %)
                                                                           :grocery-unit/unit-mass-type (keyword "mass" (:unit_mass_type %))})
                                                    (:unit_volume %) (merge {:grocery-unit/unit-volume      (:unit_volume %)
                                                                             :grocery-unit/unit-volume-type (keyword "volume" (:unit_volume_type %))})
                                                    (:unit_common %) (merge {:grocery-unit/unit-common      (:unit_common %)
                                                                             :grocery-unit/unit-common-type (keyword "common" (:unit_common_type %))}))})
                             :body
                             :id)))
        recipe-map (->> "dev/resources/seed/recipes.json"
                        (slurp)
                        (m/decode "application/json")
                        (map #(vector (:id %) (-> (make-request :post "/v1/recipes"
                                                                {:auth true
                                                                 :body (update-keys
                                                                        (merge (select-keys % [:name :instructions])
                                                                               {:amount      (:amount %)
                                                                                :amount-unit (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                      (:amount_unit %))
                                                                                :source      (or (:source %) "")})
                                                                        (fn [k] (keyword "recipe" (name k))))})
                                                  :body
                                                  :id)))
                        (into {}))
        ingredients (->> "dev/resources/seed/ingredients.json"
                         (slurp)
                         (m/decode "application/json")
                         (map #(-> (make-request :post (str "/v1/recipes/" (get recipe-map (:recipe_id %)) "/ingredients")
                                                 {:auth true
                                                  :body (update-keys {:ingredient-grocery-id (get grocery-map (:ingredient_type %))
                                                                      :amount                (:amount %)
                                                                      :amount-unit           (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                      (:amount_unit %))}
                                                                     (fn [k] (keyword "ingredient" (name k))))})

                                   :body
                                   :id)))
        recipe-map (merge recipe-map
                          (->> "dev/resources/seed/flavors.json"
                               (slurp)
                               (m/decode "application/json")
                               (map #(let [recipe-id (-> (make-request :post "/v1/recipes"
                                                                       {:auth true
                                                                        :body (update-keys (merge (select-keys % [:name :instructions])
                                                                                                  {:amount      (:amount %)
                                                                                                   :amount-unit (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                                         (:amount_unit %))
                                                                                                   :source      ""
                                                                                                   :public true})
                                                                                           (fn [k] (keyword "recipe" (name k))))})
                                                         :body
                                                         :id)]
                                       (make-request :post (str "/v1/recipes/" recipe-id "/ingredients")
                                                     {:auth true
                                                      :body (update-keys {:ingredient-recipe-id (get recipe-map (:recipe_id %))
                                                                          :amount               (:amount %)
                                                                          :amount-unit          (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                         (:amount_unit %))}
                                                                         (fn [k] (keyword "ingredient" (name k))))})

                                       [(:id %) recipe-id]))
                               (into {})))
        ingredients (concat ingredients
                            (->> "dev/resources/seed/mixins.json"
                                 (slurp)
                                 (m/decode "application/json")
                                 (map #(-> (make-request :post (str "/v1/recipes/" (get recipe-map (:flavor_id %)) "/ingredients")
                                                         {:auth true
                                                          :body (update-keys {:ingredient-recipe-id (get recipe-map (:recipe_id %))
                                                                              :amount               (:amount %)
                                                                              :amount-unit          (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                             (:amount_unit %))}
                                                                             (fn [k] (keyword "ingredient" (name k))))})
                                           :body
                                           :id))))
        menu-id (-> (make-request :post "/v1/menus"
                                  {:auth true
                                   :body {:menu/name          "test menu"
                                          :menu/active        true
                                          :menu/repeats       false
                                          :menu/duration      3
                                          :menu/duration-type :duration/day}}))

        order-map (->> "dev/resources/seed/orders.json"
                       (slurp)
                       (m/decode "application/json")
                       (map #(vector (:id %) (-> (make-request :post "/v1/orders"
                                                               {:auth true
                                                                :body (update-keys (select-keys % [:note])
                                                                                   (fn [k] (keyword "user-order" (name k))))})
                                                 :body
                                                 :id)))
                       (into {}))
        order-items (->> "dev/resources/seed/flavor_amounts.json"
                         (slurp)
                         (m/decode "application/json")
                         (map #(-> (make-request :post (str "/v1/orders/" (get order-map (:order_id %)) "/items")
                                                 {:auth true
                                                  :body {:order-item/recipe-id   (get recipe-map (:flavor_id %))
                                                         :order-item/amount      (:amount %)
                                                         :order-item/amount-unit (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                          (:amount_unit %))}})
                                   :body
                                   :id)))]
    {:grocery-map   grocery-map
     :grocery-units units
     :recipe-map    recipe-map
     :ingredients   ingredients
     :order-map     order-map
     :order-items   order-items
     :menu-id       menu-id}))

(defn init-app []
  (if (.exists (io/file users-config))
    (load-test-user)
    (make-test-users 1))
  (load-seed-data))

(ig-repl/set-prep!
 (fn []
   (when-not @db-container
     (println "resetting db")
     (reset! db-container (doto (PostgreSQLContainer. "postgres:latest") ;; Full reference to PostgreSQLContainer
                            (.withDatabaseName "test_db")
                            (.withUsername "user")
                            (.withPassword "password")
                            (.start))))
   (let [task-system (-> "resources/db-task-config.edn"
                         (db/load-config)
                         (assoc-in [:db/postgres :jdbc-url] (str (.getJdbcUrl @db-container)
                                                                 "&user=" (.getUsername @db-container)
                                                                 "&password=" (.getPassword @db-container)))
                         (db/init-system))
         migration-task (:db-tasks/migration task-system)]
     (migration-task)
     (ig/halt! task-system))
   (-> "dev/resources/server-config.edn"
       slurp
       ig/read-string
       (assoc-in [:db/postgres :jdbc-url] (str (.getJdbcUrl @db-container)
                                               "&user=" (.getUsername @db-container)
                                               "&password=" (.getPassword @db-container)))
       (ig/expand))))

(defn go
  ([]
   (go true))
  ([init-db?]
   (ig-repl/go)
   (when init-db? (init-app))))
(defn halt []
  (ig-repl/halt))
(defn reset []
  (ig-repl/reset)
  (init-app))
(defn reset-all []
  (ig-repl/reset-all)
  (init-app))

(comment
  (map #(auth0/delete-user! (:auth/auth0 state/system) (:user_id %))
       (filter #(str/starts-with? (:email %) "integration-test")
               (auth0/get-users (:auth/auth0 state/system))))
  (make-test-users 2)
  (go)
  (go false)
  (halt)
  (reset)
  (reset-all))

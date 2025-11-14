(ns user
  (:require [clj-http.client :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.auth0 :as auth0]
            [pigeon-scoops-backend.config :as config]
            [pigeon-scoops-backend.db-tasks]
            [ring.mock.request :as mock])
  (:import (java.sql Timestamp)
           (java.util UUID)
           (org.testcontainers.containers PostgreSQLContainer)))

(def user-config "dev/resources/test-user.edn")
(def db-container (atom nil))
(def token (atom nil))
(def test-user (atom nil))

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
   (let [app (-> state/system :pigeon-scoops-backend/app)
         auth (-> state/system :auth/auth0)
         response (app (-> (mock/request method uri)
                           (cond-> (:auth opts) (mock/header :authorization (str "Bearer " (or @token (get-token (conj auth @test-user)))))
                                   (:body opts) (mock/json-body (:body opts)))))
         response (update response :body (partial m/decode "application/json"))]
     response)))

(defn make-test-user []
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
                         [:manage-orders :manage-recipes :manage-groceries :manage-menus])
    (reset! token (get-token (conj auth @test-user)))
    (make-request :post "/v1/account" {:auth true})
    (spit user-config @test-user)))

(defn load-test-user []
  (println "loading token")
  (->> user-config
       (slurp)
       (edn/read-string)
       (reset! test-user)
       (conj (:auth/auth0 state/system))
       (get-token)
       (reset! token))
  (make-request :post "/v1/account" {:auth true}))

(defn load-seed-data []
  (let [grocery-map (->> "dev/resources/seed/groceries.json"
                         (slurp)
                         (m/decode "application/json")
                         (map #(vector (:type %) (-> (make-request :post "/v1/groceries"
                                                                   {:auth true
                                                                    :body {:name       (:type %)
                                                                           :department :department/grocery}})
                                                     :body
                                                     :id)))
                         (into {}))
        units (->> "dev/resources/seed/grocery_units.json"
                   (slurp)
                   (m/decode "application/json")
                   (map #(-> (make-request :post (str "/v1/groceries/" (get grocery-map (:type %)) "/units")
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
        recipe-map (->> "dev/resources/seed/recipes.json"
                        (slurp)
                        (m/decode "application/json")
                        (map #(vector (:id %) (-> (make-request :post "/v1/recipes"
                                                                {:auth true
                                                                 :body (merge (select-keys % [:name :instructions])
                                                                              {:amount      (:amount %)
                                                                               :amount-unit (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                     (:amount_unit %))
                                                                               :source      (or (:source %) "")})})
                                                  :body
                                                  :id)))
                        (into {}))
        ingredients (->> "dev/resources/seed/ingredients.json"
                         (slurp)
                         (m/decode "application/json")
                         (map #(-> (make-request :post (str "/v1/recipes/" (get recipe-map (:recipe_id %)) "/ingredients")
                                                 {:auth true
                                                  :body {:ingredient-grocery-id (get grocery-map (:ingredient_type %))
                                                         :amount                (:amount %)
                                                         :amount-unit           (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                         (:amount_unit %))}})
                                   :body
                                   :id)))
        recipe-map (merge recipe-map
                          (->> "dev/resources/seed/flavors.json"
                               (slurp)
                               (m/decode "application/json")
                               (map #(let [recipe-id (-> (make-request :post "/v1/recipes"
                                                                       {:auth true
                                                                        :body (merge (select-keys % [:name :instructions])
                                                                                     {:amount      (:amount %)
                                                                                      :amount-unit (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                            (:amount_unit %))
                                                                                      :source      ""})})
                                                         :body
                                                         :id)]
                                       (make-request :post (str "/v1/recipes/" recipe-id "/ingredients")
                                                     {:auth true
                                                      :body {:ingredient-recipe-id (get recipe-map (:recipe_id %))
                                                             :amount               (:amount %)
                                                             :amount-unit          (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                            (:amount_unit %))}})
                                       [(:id %) recipe-id]))
                               (into {})))
        ingredients (concat ingredients
                            (->> "dev/resources/seed/mixins.json"
                                 (slurp)
                                 (m/decode "application/json")
                                 (map #(-> (make-request :post (str "/v1/recipes/" (get recipe-map (:flavor_id %)) "/ingredients")
                                                         {:auth true
                                                          :body {:ingredient-recipe-id (get recipe-map (:recipe_id %))
                                                                 :amount               (:amount %)
                                                                 :amount-unit          (keyword (last (str/split (:amount_unit_type %) #"\."))
                                                                                                (:amount_unit %))}})
                                           :body
                                           :id))))
        menu-id (-> (make-request :post "/v1/menus"
                                  {:auth true
                                   :body {:name          "test menu"
                                          :active        true
                                          :repeats       false
                                          :duration      3
                                          :duration-type :duration/day}}))

        order-map (->> "dev/resources/seed/orders.json"
                       (slurp)
                       (m/decode "application/json")
                       (map #(vector (:id %) (-> (make-request :post "/v1/orders"
                                                               {:auth true
                                                                :body (select-keys % [:note])})
                                                 :body
                                                 :id)))
                       (into {}))
        order-items (->> "dev/resources/seed/flavor_amounts.json"
                         (slurp)
                         (m/decode "application/json")
                         (map #(-> (make-request :post (str "/v1/orders/" (get order-map (:order_id %)) "/items")
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
     :order-items   order-items
     :menu-id       menu-id}))

(defn init-app []
  (if (.exists (io/file user-config))
    (load-test-user)
    (make-test-user))
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
                          (config/load-config)
                          (assoc-in [:db/postgres :jdbc-url] (str (.getJdbcUrl @db-container)
                                                                  "&user=" (.getUsername @db-container)
                                                                  "&password=" (.getPassword @db-container)))
                          (config/init-system))
          migration-task (:tasks/migration task-system)]
      (migration-task)
      (ig/halt! task-system))
    (-> "dev/resources/server-config.edn"
        slurp
        ig/read-string
        (assoc-in [:db/postgres :jdbc-url] (str (.getJdbcUrl @db-container)
                                                "&user=" (.getUsername @db-container)
                                                "&password=" (.getPassword @db-container)))
        (ig/expand))))

(defn go []
  (ig-repl/go)
  (init-app))
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
  (sql/query (:db/postgres state/system) ["SELECT ingredient.*, recipe.name, grocery.name
                                           FROM ingredient
                                           LEFT JOIN recipe ON ingredient.ingredient_recipe_id = recipe.id
                                           LEFT JOIN grocery ON ingredient.ingredient_grocery_id = grocery.id
                                           WHERE ingredient.recipe_id = (?);"
                                          #uuid"93509207-f5b1-4996-9d51-e39f328c7371"])
  (sql/query (:db/postgres state/system) ["SELECT *
                                           FROM ingredient
                                           WHERE recipe_id = (?);"
                                          #uuid"93509207-f5b1-4996-9d51-e39f328c7371"])
  (let [user (sql/insert! (:db/postgres state/system) :account {:id   (str (UUID/randomUUID))
                                                                :name "greg"})

        chocolate (sql/insert! (:db/postgres state/system) :recipe {:id           (UUID/randomUUID)
                                                                    :name         "chocolate"
                                                                    :public       true
                                                                    :amount       3
                                                                    :amount-unit  "cup"
                                                                    :user-id      (:account/id user)
                                                                    :instructions (into-array String [])})
        vanilla (sql/insert! (:db/postgres state/system) :recipe {:id           (UUID/randomUUID)
                                                                  :name         "vanilla"
                                                                  :public       true
                                                                  :amount       3
                                                                  :amount-unit  "cup"
                                                                  :user-id      (:account/id user)
                                                                  :instructions (into-array String [])})
        inactive-menu (sql/insert! (:db/postgres state/system) :menu {:id            (UUID/randomUUID)
                                                                      :name          "food"
                                                                      :duration      4
                                                                      :duration-type "day"
                                                                      :end-time      (Timestamp/from (java.time.Instant/now))})
        active-menu (sql/insert! (:db/postgres state/system) :menu {:id            (UUID/randomUUID)
                                                                    :name          "food"
                                                                    :active        true
                                                                    :duration      4
                                                                    :duration-type "day"
                                                                    :end-time      (Timestamp/from (java.time.Instant/now))})
        inactive-item (sql/insert! (:db/postgres state/system) :menu-item {:id        (UUID/randomUUID)
                                                                           :menu-id   (:menu/id inactive-menu)
                                                                           :recipe-id (:recipe/id chocolate)})
        active-item (sql/insert! (:db/postgres state/system) :menu-item {:id        (UUID/randomUUID)
                                                                         :menu-id   (:menu/id active-menu)
                                                                         :recipe-id (:recipe/id vanilla)})
        inactive-sizes (mapv #(sql/insert! (:db/postgres state/system) :menu-item-size {:id           (UUID/randomUUID)
                                                                                        :menu-id      (:menu/id inactive-menu)
                                                                                        :menu-item-id (:menu-item/id inactive-item)
                                                                                        :amount       %
                                                                                        :amount-unit  "cup"})
                             (range 1 4))
        active-sizes (mapv #(sql/insert! (:db/postgres state/system) :menu-item-size {:id           (UUID/randomUUID)
                                                                                      :menu-id      (:menu/id active-menu)
                                                                                      :menu-item-id (:menu-item/id active-item)
                                                                                      :amount       %
                                                                                      :amount-unit  "cup"})
                           (range 1 4))]
    (sql/query (:db/postgres state/system) (-> (h/select :menu-item-size/*)
                                               (h/from :menu-item-size)
                                               (h/join :menu [:= :menu/id :menu-item-size/menu-id])
                                               (h/where [:= :menu/active true])
                                               (hsql/format))))

  (ig-repl/go)
  (sql/find-by-keys (:db/postgres state/system) :menu :all)
  @token
  (make-test-user)
  (load-test-user)
  (let [recipe-id (-> (make-request :get "/v1/recipes" {:auth true})
                      :body
                      :private
                      (first)
                      (:recipe/id))
        menu-id (-> (make-request :post "/v1/menus" {:auth true
                                                     :body {:name          "foobar"
                                                            :repeats       false
                                                            :active        true
                                                            :duration      4
                                                            :duration-type :duration/day}})
                    :body
                    :id)
        menu-item-id (-> (make-request
                           :post
                           (str "/v1/menus/" menu-id "/items")
                           {:auth true
                            :body {:recipe-id recipe-id}})
                         :body
                         :id)
        menu (-> (make-request :get (str "/v1/menus") {:auth true})
                 :body
                 :id)]


    [recipe-id menu-id menu-item-id menu])
  (str (.getJdbcUrl @db-container)
       "&user=" (.getUsername @db-container)
       "&password=" (.getPassword @db-container))
  (init-app)
  (go)
  (halt)
  (reset)
  (reset-all))

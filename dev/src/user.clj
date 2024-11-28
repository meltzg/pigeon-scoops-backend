(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
            [muuntaja.core :as m]
            [next.jdbc.sql :as sql]
            [pigeon-scoops-backend.server]))

(ig-repl/set-prep!
  (fn []
    (-> "resources/config.edn"
        slurp
        ig/read-string)))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(def app (-> state/system :pigeon-scoops-backend/app))
(def db (-> state/system :db/postgres))

(comment
  (-> (app {:request-method :get
            :uri            "/v1/recipes/1c449b14-cf10-4295-b2b5-bdf12d5f55cf"})
      :body
      (slurp))
  (-> (app {:request-method :delete
            :uri            "/v1/recipes/1c449b14-cf10-4295-b2b5-bdf12d5f55cf"}))
  (-> (app {:request-method :put
            :uri            "/v1/recipes/a3dde84c-4a33-45aa-b0f3-4bf9ac997680"
            :body-params    {:name      "My recipe is great"
                             :prep-time 49
                             :img       "image-url"
                             :public    true}}))
  (->> (app {:request-method :get
             :uri            "/v1/recipes"})
       :body (slurp)
       (m/decode "application/json"))
  (->> (app (-> {:request-method :post
                 :uri            "/v1/recipes"
                 :body-params    {:name      "My recipe"
                                  :prep-time 49
                                  :img       "image-url"}}
                (mock/header :authorization (str "Bearer " (auth0/get-test-token)))
                (mock/header :accept "application/transit+json")))
       :body
       (slurp)
       (m/decode "application/transit+json"))
  (-> (sql/find-by-keys db :recipe {:public false}))
  (go)
  (halt)
  (reset)
  (reset-all))

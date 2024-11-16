(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [integrant.repl.state :as state]
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
            :uri            "/v1/recipes/a3dde84c-4a33-45aa-b0f3-4bf9ac997680"})
      :body
      (slurp))
  (-> (app {:request-method :get
            :uri            "/v1/recipes"})
      :body
      (slurp))
  (-> (app {:request-method :post
            :uri            "/v1/recipes"
            :body-params    {:name      "My recipe"
                             :prep-time 49
                             :img       "image-url"}})
      :body
      (slurp))
  (go)
  (halt)
  (reset)
  (reset-all))

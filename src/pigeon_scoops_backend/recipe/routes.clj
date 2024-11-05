(ns pigeon-scoops-backend.recipe.routes)

(defn routes [env]
  ["/recipes" {:get {:handler (fn [req] {:status 200
                                         :body   "Hello world"})}}])

(ns pigeon-scoops-backend.cleanup
  (:require
   [integrant.repl.state :as state]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [pigeon-scoops-backend.auth :as auth0]
   [pigeon-scoops-backend.test-system :refer [system-fixture test-users-file]]
   [pigeon-scoops-backend.server]))

(defn delete-test-auth0-accounts! []
  (println "deleting integration test users")
  (->> test-users-file
       (slurp)
       (edn/read-string)
       (map :uid)
       (map #(do
               (println "Deleting user with uid" %)
               (auth0/delete-user! (assoc (:auth/auth0 state/system)
                                          :skip-auth0-delete?
                                          false)
                                   %)))
       (doall))

  (io/delete-file (io/file test-users-file)))

(defn -main []
  (system-fixture delete-test-auth0-accounts!))

(ns pigeon-scoops-backend.utils
  (:require [next.jdbc :as jdbc]))

(defn db-str->keyword [entity & keys]
  (reduce (fn [acc k] (if (contains? entity k)
                        (update acc k keyword)
                        acc))
          entity keys))

(defn keyword->db-str [entity & keys]
  (reduce (fn [acc k]
            (if (contains? entity k)
              (update acc k #(subs (str %) 1))
              acc))
          entity keys))

(defn with-connection [db f]
  (try
    (with-open [conn (jdbc/get-connection db)]
      (let [conn-opts (jdbc/with-options conn (:options db))]
        (f conn-opts)))
    (catch IllegalArgumentException _
      (f db))))


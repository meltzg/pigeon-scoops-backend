(ns pigeon-scoops-backend.utils)

(defn db-str->keyword [entity & keys]
  (reduce (fn [acc k] (update acc k keyword)) entity keys))

(defn keyword->db-str [entity & keys]
  (reduce (fn [acc k]
            (if (contains? entity k)
              (update acc k #(subs (str %) 1))
              acc))
          entity keys))

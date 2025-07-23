(ns pigeon-scoops-backend.utils
  (:require [next.jdbc :as jdbc])
  (:import (java.time Duration ZonedDateTime)))

(defn keyword->db-str [k]
  (subs (str k) 1))

(defn apply-db-str->keyword [entity & keys]
  (reduce (fn [acc k] (if (contains? entity k)
                        (update acc k keyword)
                        acc))
          entity keys))

(defn apply-keyword->db-str [entity & keys]
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

(defn end-time [duration duration-type]
  (let [now (ZonedDateTime/now)]
    (case duration-type
      :duration/day (.plus now (Duration/ofDays duration))
      :duration/week (.plusWeeks now duration)
      :duration/month (.plusMonths now duration))))

(defn remove-nil-keys [value]
  (cond (map? value)
        (update-vals
          (->> value
               (remove (comp nil? val))
               (into {}))
          remove-nil-keys)
        (coll? value)
        (map remove-nil-keys value)
        :else
        value))

(defn doall-deep [value]
  (cond (map? value)
        (update-vals value doall-deep)
        (or (coll? value) (seq? value))
        (mapv doall-deep value)
        :else
        value))

(comment
  (keyword->db-str :foo/bar))

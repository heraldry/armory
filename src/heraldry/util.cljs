(ns heraldry.util)

(def -current-id
  (atom 0))

(defn reset-id []
  (reset! -current-id 0))

(defn id [prefix]
  (str prefix "_" (swap! -current-id inc)))
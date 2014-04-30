(ns caterpillar.storage
  (:use
   korma.core
   [korma.db :only (defdb)])
  (:require
   [environ.core :refer [env]]
   ))

(def locks (atom {}))

(defn get-lock [entity]
  (or (entity @locks) (entity (swap! locks (fn [l] (if (entity l) l (assoc l entity (Object.))))))))

(defn initialize [conf]
  (defdb db conf))

;(initialize (env :database))

(defn prepare-data [o & [persistance-fields]]
  (let [o (if (seq persistance-fields) (select-keys o persistance-fields) o)]
  (->> o
       (map (fn [[k v]] (cond (keyword? v) [k (str v)]
                              (coll? v) [k (pr-str v)]
                              :else [k v])))
       (into {}))))

(defn exists?! [o entity]
  (let [o (prepare-data o)]
    (locking (get-lock entity)
      (if-let [found (and (seq o) (seq (select entity (where o))))]
        found
        (do (insert entity (values o)) false)))))

(defn insert-or-update [o entity composite-key persistent-fields]
  (let [o (prepare-data o persistent-fields)
        k (select-keys o composite-key)]
    (locking (get-lock entity)
      (if (and (seq k) (seq (select entity (where k))))
        (update entity (set-fields o)(where k))
        (insert entity (values o))))))

(defn insert-only [o entity & [persistent-fields]]
  (let [o (prepare-data o persistent-fields)]
    (insert entity (values o))))


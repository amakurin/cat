(ns caterpillar.storage
  (:use
   korma.core
   [korma.db :only (defdb)])
  (:require
   [caterpillar.tools :as tools]
   [environ.core :refer [env]]
   ))

(def locks (tools/lock-provider))

(defn initialize [conf]
  (when-not (resolve 'db)
    (defdb db conf)))

;(initialize (env :database))
;(defdb db (env :database))

(defn prepare-data [o & [persistance-fields]]
  (let [o (if (seq persistance-fields) (select-keys o persistance-fields) o)]
  (->> o
       (map (fn [[k v]] (cond (keyword? v) [k (str v)]
                              (coll? v) [k (pr-str v)]
                              :else [k v])))
       (into {}))))

(defn exists?! [o entity]
  (let [o (prepare-data o)]
    (locking (tools/get-lock locks entity)
      (if-let [found (and (seq o) (seq (select entity (where o))))]
        found
        (do (insert entity (values o)) false)))))

(defn insert-or-update [o entity composite-key persistent-fields]
  (let [o (prepare-data o persistent-fields)
        k (select-keys o composite-key)]
    (locking (tools/get-lock locks entity)
      (if (and (seq k) (seq (select entity (where k))))
        (update entity (set-fields o)(where k))
        (insert entity (values o))))))

(defn insert-only [o entity & [persistent-fields]]
  (let [o (prepare-data o persistent-fields)]
    (insert entity (values o))))


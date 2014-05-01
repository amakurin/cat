(ns caterpillar.classifier
  (:use korma.core)
  (:require
   [caterpillar.formatters :as form]
   [caterpillar.config :as conf]
   [caterpillar.storage :as store]
   [caterpillar.tools :as tools]
   [caterpillar.errors :as err]
   [environ.core :refer [env]]
   [cronj.core :as sched]
   [cronj.data.scheduler :as ts]
   [taoensso.timbre :as timbre]
   [clojure.string :as s]
   [clj-time.core :as tc]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   ))

(defn get-rate[input rate-weights]
  (let [weights (->> rate-weights
                     (mapcat (fn [[k [f t]]] [[[k false] f]
                                              [[k true] t]
                                              [[k nil] 0]]))
                     (into {}))]
    (->> (:fields settings)
         keys
         (map (fn [k] [k (k input)]))
         (map #(get weights %))
         (reduce +)
         )))

(defn count-distinct [coll]
  (->> coll (group-by :src-id))
  count)

(time(->>
 (select :ads (fields :id :src-id :target :extracted-edn :classified)
         (where  {:created [> (tf/unparse (tf/formatters :mysql)
                                          (tc/minus (tl/local-now)
                                                    (tc/days 6)))]
                  :extracted 1}))
 (mapcat (fn [{:keys [id src-id extracted-edn classified] :as x}]
        (let [extracted (read-string extracted-edn)]
          (-> x
              (dissoc :extracted-edn)
              (merge {:extracted extracted
                      :phone (:phone extracted)})
              (tools/do-split-by [:phone])))
           ))
 (group-by :phone)
 (mapcat (fn [[k v]]
           (let [ids (distinct (map :src-id v))]
             (map #(assoc-in % [:phone-entries k] ids)v))))
 (group-by :src-id)
 (map (fn [[k v]]
        (let [fst (first v)]
          (merge fst
                 {:phone-entries
                  (->> v
                       (mapcat #(:phone-entries %))
                       (mapcat (fn [[ph ids]] ids))
                       distinct count)
                  :rate (get-rate (:extracted fst))
                  }
                 ))))
;;  (filter (fn [{:keys [rate phone-entries extracted]}]
;;             (or (>= rate 0.5))))
;;  (filter (fn [{:keys [rate phone-entries extracted]}]
;;            (and (>= rate 0.1)(<= rate 0.5))))
;; (filter (fn [{:keys [rate phone-entries extracted]}]
;;           (and (> phone-entries 1) (< phone-entries 5))))
(filter (fn [{:keys [rate phone-entries extracted]}]
          (and (< rate 0.5) (= phone-entries 1))))
   count
;; (map (fn [{:keys [rate target phone-entries extracted]}]
;;        (println "===============")
;;        (println rate phone-entries target)
;;        (println extracted)
;;        ))

 )
     )






























































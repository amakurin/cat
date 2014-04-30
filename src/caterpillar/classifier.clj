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

(def settings
  {:fields
   {:by-fact [0 0.9]
    :to-agent [0 0.5]
    :percent [0 0.5]
    :comission [-0.4 0.4]
    :advert-marker [0 0.6]
    :absurd-phone [0 0.8]
    :owner [0.2 -0.4]
    :middleman [-0.2 0]
    :distrub [-0.1 0]
    }
   })

(defn get-rate[input]
  (let [weights (->> (:fields settings)
                     (mapcat (fn [[k [f t]]] [[[k false] f]
                                              [[k true] t]
                                              [[k nil] 0]]))
                     (into {}))
        ]
    (->> (:fields settings)
         keys
         (map (fn [k] [k (k input)]))
         (map #(get weights %))
         (reduce +)
         );weights
    ))

(get-rate {:by-fact true
           :owner true
           :middleman false})

(->>
(select :ads (where {:extracted 1}))
(map #(->> % :extracted-edn read-string))
;(map (fn [x] [(get-rate x) x]))
;(filter #(and (> (first %) 0.3)(< (first %) 0.7)))
;(filter #(and (< (first %) 0.5)))
;(filter #(and (> (first %) 0.5)))
;; (filter #(or (nil? (:lat %))
;;              (nil? (:lng %))))
 count
;; (map (fn [[r o]]
;;        (println "===============")
;;        (println r)
;;        (println o)
;;        ))

 )

(tf/show-formatters)
(exec-raw ["SELECT *
           FROM ads
           WHERE created > ( CURRENT_DATE - INTERVAL 1 WEEK )"]
          :results)

(defn count-distinct [coll]
  (->> coll (group-by :src-id))
  count)

(time(->>
 (select :ads (fields :id :src-id :target :extracted-edn :classified)
         (where  {:created [> (tf/unparse (tf/formatters :mysql)
                                          (tc/minus (tl/local-now)
                                                    (tc/days 4)))]
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






























































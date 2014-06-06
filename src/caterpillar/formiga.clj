(ns caterpillar.formiga
  (:use korma.core)
  (:require
   [caterpillar.formatters :as form]
   [caterpillar.config :as conf]
   [caterpillar.storage :as store]
   [caterpillar.system :as sys]
   [caterpillar.tools :as tools]
   [caterpillar.errors :as err]
   [environ.core :refer [env]]
   [cronj.core :as sched]
   [cronj.data.scheduler :as ts]
   [taoensso.timbre :as ti]
   [clojure.string :as s]
   [clj-time.core :as tc]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   ))

(defn check-bounds [bounds v]
  (->> bounds
       (filter (fn [[k [v1 v2]]] (and (>= v v1) (< v v2))))
       first first))

(defn get-rate[input {:keys [rate-weights multi-phone-bounds]
                      :or {multi-phone-bounds {0 [-999 999]}} :as conf}]
  (let [weights (->> rate-weights
                     (mapcat (fn [[k [f t]]] [[[k false] f]
                                              [[k true] t]
                                              [[k nil] 0]]))
                     (into {}))]
    (->> rate-weights
         keys
         (map (fn [k] [k (k input)]))
         (map #(get weights %))
         (reduce +)
         (#(+ % (check-bounds multi-phone-bounds (-> input :phone count))))
         )))

(defn count-distinct [coll]
  (->> coll (group-by :src-id))
  count)

(defn get-inagents-count [phones]
  (if phones
    (->
     (select :agents
             (aggregate (count :*) :cnt)
             (where {:phone [in phones]}))
     first :cnt)
    -1))

(defn merge-verdict [{:keys [rate-bounds appearance-bounds] :as conf}
                     {:keys [phone-entries agent-rate
                             agent-phones extracted] :as ad}]
  (let [absurd? (:absurd-phone extracted)
        rate-check (check-bounds rate-bounds agent-rate)
        appearance-check (check-bounds appearance-bounds phone-entries)]

    (merge ad
           {:history-edn (pr-str {:phone-entries phone-entries
                                  :agent-rate agent-rate
                                  :agent-phones agent-phones
                                  :absurd-phone absurd?})
            :verdict
            (cond (< agent-phones 0) 6
                  (> agent-phones 0) 10
                  absurd? 9
                  (or (= :agent rate-check)(= :agent appearance-check)) 10
                  (= :owner rate-check appearance-check) 0
                  :else 5
                  )})))

(defn classify [{:keys [city storage-entity-src sys] :as task-opts}]
  (let [{:keys [query-days] :or {query-days 2} :as conf} (sys/get-config-data sys)]
  (->>
   (select storage-entity-src (fields :id :src-id :target :extracted-edn :verdict :url :city)
           (where  {:created [> (tf/unparse (tf/formatters :mysql)
                                            (tc/minus (tl/local-now)
                                                      (tc/days query-days)))]
                    :extracted 1
                    :verdict [<= 5]}))
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
                    :agent-rate (get-rate (:extracted fst) conf)
                    :agent-phones (->> fst :extracted :phone get-inagents-count)
                    }
                   ))))
   (map #(merge-verdict conf %)))))

(def locks (atom {}))

(defn get-lock [entity]
  (or (entity @locks) (entity (swap! locks (fn [l] (if (entity l) l (assoc l entity (Object.))))))))

(defn push-agent [ad]
  (doseq
    [x (-> ad (merge {:phone (-> ad :extracted :phone)
                      :updates (:phone-entries ad)})
           (tools/do-split-by [:phone]))]
    (err/with-try
     {:link x :step :classify-push-agent}
     (store/insert-or-update x
                             :agents
                             [:phone]
                             [:target :phone :url :city :updates]))))

(defn
  ^{:task-handler true}
  task-handler [t {:keys [task-id
                          storage-entity-src
                          sys] :as opts}]
  (locking (get-lock task-id)
    (let [ads (classify opts)]
      (doseq [ad ads]
        (when (and (= 10 (:verdict ad)) (= 0 (:agent-phones ad)) (not (get-in ad [:extracted :absurd-phone])))
          (push-agent ad)
          )
        (err/with-try
         {:link ad :step :update-verdict}
         (update storage-entity-src
                 (set-fields {:verdict (:verdict ad)
                              :history-edn (:history-edn ad)})
                 (where {:id (:id ad)}))))
      (ti/info "Formiga " task-id " handler procceed count: " (count ads))
      )))

(def system (sys/subsystem :formiga))

;; (sys/init system)
;; (sys/get-config-data system)

;; (sys/start system)
;; (sys/stop system)

;; (task-handler nil
;;               {:task-id :x
;;                :city :smr
;;                :storage-entity-src :ads
;;                :sys system})






















































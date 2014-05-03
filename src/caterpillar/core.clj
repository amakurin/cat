(ns caterpillar.core
  (:require
   [caterpillar.processors :as proc]
   [caterpillar.config :as conf]
   [caterpillar.system :as sys]
   [caterpillar.storage :as store]
   [caterpillar.tools :as tools]
   [caterpillar.errors :as err]
   [net.cgrand.enlive-html :as html]
   [environ.core :refer [env]]
   [cronj.core :as sched]
   [cronj.data.scheduler :as ts]
   [taoensso.timbre :as timbre]
   ))

(defn random-sleep [[x y]]
  (let [minute 1000
        x (* minute x)
        y (* minute y)
        r (+ x (rand-int (inc (- y x))))]
    (Thread/sleep r)))

(defn seen? [{:keys [src-id target] :as link}]
  (assert (and src-id target) "Link should be map of at least :src-id :target.")
  (err/with-try {:step :seen-lookup :link link}
    (store/exists?! (select-keys link [:src-id :target]) :seen)))

(defn do-step [link {:keys [storage-entity
                            store-option
                            insert-or-update-key
                            merge-data
                            as-edn-to
                            split-by
                            filter-by
                            persistent-fields
                            persistent-fields-except
                            ] :as opts}]
  (let [persistent-fields (if (seq persistent-fields-except)
                            (remove #((set persistent-fields-except) %) (or persistent-fields (keys link)))  persistent-fields)
        links (-> link
                  (merge merge-data (when as-edn-to {as-edn-to (pr-str link)}))
                  (tools/do-split-by split-by)
                  (tools/do-filter-by filter-by))]
    (doseq [link links]
      (err/with-try {:link link :opts opts}
        (cond (= store-option :so-insert-or-update)
              (store/insert-or-update link
                                      storage-entity
                                      insert-or-update-key
                                      persistent-fields)
              :else
              (store/insert-only link
                                 storage-entity
                                 persistent-fields))))))

(defn process-link [{:keys [target] :as link} {:keys [merge-data processing sys] :as opts}]
  (let [conf (sys/get-config-data sys)
        link (if target (err/with-try {:link link :opts opts}
                          (proc/process-target target link conf)) link)
        link (merge link merge-data)
        {:keys [steps pause]} processing]
    (doseq [step steps] (do-step link step))
    (timbre/info "Success with link "link)
    (when pause (random-sleep pause))))

(defn
  ^{:task-handler true}
  crawl-handler [t {:keys [task-id target data sys] :as opts}]
  (let [conf (sys/get-config-data sys)
        links (err/with-try {:link data :opts opts}
                (proc/process-target target data conf))]
    (timbre/info task-id " processed target "target ", found links count: " (count links))
    (doseq [link links]
      (when-not (seen? link) (process-link link opts)))))

(def system (sys/subsystem :caterpillar))

;; (sys/init system)
;; (sys/get-config-data system)

;; (sys/start system)
;; (sys/stop system)

;; (time
;;  (task-handler nil {:task-id :sdf :storage-entity :ads-bu :sys system})
;; )


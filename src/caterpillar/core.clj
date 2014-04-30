(ns caterpillar.core
  (:require
   [caterpillar.processors :as proc]
   [caterpillar.config :as conf]
   [caterpillar.storage :as store]
   [caterpillar.tools :as tools]
   [caterpillar.errors :as err]
   [net.cgrand.enlive-html :as html]
   [environ.core :refer [env]]
   [cronj.core :as sched]
   [cronj.data.scheduler :as ts]
   [taoensso.timbre :as timbre]
   ))

(def sys (atom {}))

(defn get-config[]
  (conf/cget (:conf @sys)))

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

(defn process-link [{:keys [target] :as link} {:keys [merge-data processing] :as opts}]
  (let [link (if target (err/with-try {:link link :opts opts}
                          (proc/process-target target link (get-config))) link)
        link (merge link merge-data)
        {:keys [steps pause]} processing]
    (doseq [step steps] (do-step link step))
    (timbre/info "Success with link "link)
    (when pause (random-sleep pause))))

(defn crawl-handler [t {:keys [task-id target data processing] :as opts}]
  (let [links (err/with-try {:link data :opts opts}
                (proc/process-target target data (get-config)))]
    (timbre/info task-id " processed target "target ", found links count: " (count links))
    (doseq [link links]
      (when-not (seen? link) (process-link link opts)))))

(defn create-task [task-id]
  (let [{:keys [sched opts]:as task-conf} (get-in (get-config) [:tasks task-id])]
    {:id task-id
     :handler crawl-handler
     :schedule sched
     :opts (assoc opts :task-id task-id)}))

(defn init []
  (if (:started @sys)
    (timbre/info "Crawl already started. Use restart or stop.")
    (do
      (timbre/info "Crawl initialization...")
      (store/initialize (env :database))
      (reset! sys {:conf (conf/file-config (env :crawl-config))})
      (let [conf (get-config)
            tasks (map (fn [[k v]] (create-task k))(:tasks conf))]
        (swap! sys assoc :cronj (sched/cronj :entries tasks))))))

(defn start []
  (if (:started @sys)
    (timbre/info "Crawl already started.")
    (do
      (timbre/info "Crawl starting...")
      (if-let [cronj (:cronj @sys)]
        (sched/start! cronj)
        (do (init)(start)))
      (swap! sys assoc :started true)
      (timbre/info "Crawl started."))))

(defn stop []
  (if (:started @sys)
    (do
      (when-let [cronj (:cronj @sys)] (sched/stop! cronj))
      (timbre/info "Crawl stopped."))
    (timbre/info "Crawl already stopped."))
  (swap! sys dissoc :started))

(defn restart []
  (stop)
  (conf/creset!(:conf @sys))
  (start))

;(init)
;(start)
;(stop)
;(restart)

(defn -main [& args]
  (println "Working!")
  )

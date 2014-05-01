(ns caterpillar.system
  (:require
   [caterpillar.config :as conf]
   [caterpillar.storage :as store]
   [caterpillar.tools :as tools]
   [clj-time.core :as tc]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [cronj.core :as sched]
   [environ.core :refer [env]]
   [taoensso.timbre :as ti]
   )
 )
;; ok in order code not to look realy trashy
;; i've decided to implement this temporary abstraction for subsystem management
;; we take configs from project settings
;; everyone of which may or may not contain tasks section
;; if task descriptors are found we use them with scheduler
;; we also use namespace of subsystem to find fns with meta
;; - task-handler Required if task descriptors are specified
;; - system-initializer Optional

;;         :systems {:caterpillar
;;                   {:config "conf/crawl.clj"
;;                    :namespace caterpillar.core}
;;                   :mariposa
;;                   {:config "conf/extract.clj"
;;                    :namespace caterpillar.mariposa}
;;                   }

;; (defn
;;   ^{:task-handler? true}
;;   extract-handler [t {:keys [task-id storage-entity] :as opts}]
;;   (locking (get-lock task-id)
;;     (let [conf (get-config)
;;           found (select storage-entity (fields :id :raw-edn) (where {:extracted 0}))]
;;       (doseq [{:keys [id raw-edn] :as item} found]
;;         (err/with-try {:link-id id}
;;                       (let [edn (->> raw-edn
;;                                      read-string
;;                                      (process-item conf)
;;                                      correct-person-name)]
;;                         (update storage-entity
;;                                 (set-fields {:extracted 1
;;                                              :extracted-edn (pr-str edn)})
;;                                 (where {:id id})))))
;;       (ti/info "Mariposa extract-handler procceed count: " (count found)))))

;; ;; (time
;; ;;  (extract-handler nil {:task-id :sdf :storage-entity :ads-bu})
;; ;; )

;; ;; (select :ads-bu (fields [:id :raw-edn]) (where {:extracted 0}))

;; ;; (->> (select :ads-bu (fields :id))
;; ;;      (take 100)
;; ;;      (map #(update :ads-bu (set-fields {:extracted 0})(where {:id (:id %)}))))

(defn get-fn [ns k]
  (->> (tools/get-funcs ns k) first val :fn-self))

(defn create-task [task-id conf task-handler]
  (let [{:keys [sched opts]:as task-conf} (get-in conf [:tasks task-id])]
    {:id task-id
     :handler task-handler
     :schedule sched
     :opts (assoc opts :task-id task-id)}))

(defn init-internal [sys {:keys [sys-name
                                 sys-ns
                                 config-file] :as metasys}]
  (if (:started @sys)
    (ti/info (str sys-name " already started. Use restart or stop."))
    (let [sys-merge (get-fn sys-ns :system-merge)
          task-handler (get-fn sys-ns :task-handler)
          init-config (get-fn sys-ns :init-config)]
      (ti/info (str sys-name " initialization..."))
      (store/initialize (env :database))
      (let [conf-state (conf/file-config config-file)
            conf (if init-config (conf/cswap! conf-state init-config) (conf/cget conf-state))
            tasks (map (fn [[k v]] (create-task k conf task-handler))(:tasks conf))]
        (reset! sys (merge
                     {:conf conf-state
                      :cronj (sched/cronj :entries tasks)}
                     (when sys-merge (sys-merge))))
        ))))

(defn start-internal [sys {:keys [sys-name
                        sys-ns
                        config-file] :as metasys}]
  (if (:started @sys)
    (ti/info (str sys-name " already started."))
    (do
      (ti/info "Publisher starting...")
      (if-let [cronj (:cronj @sys)]
        (sched/start! cronj)
        (do (init-internal sys metasys)(start-internal sys metasys)))
      (swap! sys assoc :started true)
      (ti/info (str sys-name " started.")))))

(defn stop-internal [sys {:keys [sys-name
                                 sys-ns
                                 config-file] :as metasys}]
  (if (:started @sys)
    (do
      (when-let [cronj (:cronj @sys)] (sched/stop! cronj))
      (ti/info (str sys-name " stopped.")))
    (ti/info (str sys-name " already stopped.")))
  (swap! sys dissoc :started))

(defn restart-internal [sys {:keys [sys-name
                                    sys-ns
                                    config-file] :as metasys}]
  (stop-internal sys metasys)
  (conf/creset! (:conf @sys))
  (start-internal sys metasys))

;; ;(init)
;; ;(start)
;; ;(stop)
;; ;(restart)

(defprotocol ISubsystem
  (init [_])
  (start [_])
  (stop [_])
  (restart [_])
  (get-config [_])
  (get-config-data [_]))

(defn subsystem [sys-id]
  (if-let [{:keys [sys-name
                   sys-ns
                   config-file] :as metasys} (->> (env :subsystems) (filter (fn [{:keys [id]}] (= id sys-id))) first)]
    (let [state (atom {:conf nil
                       :sys nil
                       :started nil})]
      (reify
        ISubsystem
        (init [_] (init-internal state metasys))
        (start [_] (start-internal state metasys))
        (stop [_] (stop-internal state metasys))
        (restart [_] (restart-internal state metasys))
        (get-config [_] (:conf @state))
        (get-config-data [_] (conf/cget (:conf @state))))
      )
    (println "Can't find config for subsystem id: "sys-id)
    )
  )


(defn -main [& args]
  (println "Working!")
  )

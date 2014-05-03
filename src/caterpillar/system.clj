(ns caterpillar.system
  (:require
   [caterpillar.config :as conf]
   [caterpillar.storage :as store]
   [caterpillar.tools :as tools]
   [clj-time.core :as tc]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [cronj.core :as sched]
   [cronj.data.scheduler :as ts]
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
(defn get-fn [ns k]
  (when-let [f (first (tools/get-funcs ns k))]
    (->> f val :fn-self)))

(defn schedule-task [cj task]
    (ts/schedule-task (:scheduler cj) (ts/task-entry task)))

(defn create-task [task-id conf sys task-handler]
  (let [{:keys [sched opts]:as task-conf} (get-in conf [:tasks task-id])]
    {:id task-id
     :handler task-handler
     :schedule sched
     :opts (merge opts {:task-id task-id
                        :sys sys})}))

(declare get-cronj)
(defn init-internal [state sys {:keys [sys-name
                                 sys-ns
                                 config-file] :as metasys}]
    (if (:started @state)
      (ti/info (str sys-name " already started. Use restart or stop."))
      (let [sys-merge (get-fn sys-ns :system-merge)
            task-handler (get-fn sys-ns :task-handler)
            init-config (get-fn sys-ns :init-config)]
        (ti/info (str sys-name " initialization..."))
        (store/initialize (env :database))
        (let [conf-state (conf/file-config config-file)
              conf (if init-config (conf/cswap! conf-state init-config) (conf/cget conf-state))
              cronj (get-cronj sys)]
          (reset! state
                  (merge
                   {:conf conf-state}
                   (when sys-merge (sys-merge))))
          (doall (map (fn [[k v]] (schedule-task cronj (create-task k conf sys task-handler)))(:tasks conf)))
          @state))))

(defn start-internal [sys {:keys [sys-name
                                  sys-ns
                                  config-file] :as metasys}]
  (let [state (:state sys)]
    (if (:started @state)
      (ti/info (str sys-name " already started."))
      (do
        (ti/info (str sys-name " starting..."))
        (if-let [cronj (:cronj sys)]
          (sched/start! cronj)
          (do (init-internal sys metasys)(start-internal sys metasys)))
        (swap! state assoc :started true)
        (ti/info (str sys-name " started."))))))

(defn stop-internal [sys {:keys [sys-name
                                 sys-ns
                                 config-file] :as metasys}]
  (let [state (:state sys)]
    (if (:started @state)
      (do
        (when-let [cronj (:cronj sys)] (sched/stop! cronj))
        (ti/info (str sys-name " stopped.")))
      (ti/info (str sys-name " already stopped.")))
    (swap! state dissoc :started)))

(defn restart-internal [sys {:keys [sys-name
                                    sys-ns
                                    config-file] :as metasys}]
  (stop-internal sys metasys)
  (conf/creset! (:conf (->> sys :state deref)))
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
  (get-config-data [_])
  (get-task-ids [_])
  (get-cronj [_])
  (get-state [_ korks]))

(defn subsystem [sys-id]
  (if-let [{:keys [sys-name
                   sys-ns
                   config-file] :as metasys} (->> (env :subsystems) (filter (fn [{:keys [id]}] (= id sys-id))) first)]
    (let [{:keys [cronj state] :as sys}
          {:cronj (sched/cronj :entries [])
           :state (atom {})}]
      (reify
        ISubsystem
        (init [this] (init-internal state this metasys))
        (start [_] (start-internal sys metasys))
        (stop [_] (stop-internal sys metasys))
        (restart [_] (restart-internal sys metasys))
        (get-config [_] (:conf @state))
        (get-config-data [_] (conf/cget (:conf @state)))
        (get-task-ids [_] (sched/get-ids cronj))
        (get-cronj [_] cronj)
        (get-state [_ korks] (if (coll? korks) (get-in @state korks) (get @state korks))))
      )
    (println "Can't find config for subsystem id: "sys-id)
    )
  )


(defn -main [& args]
  (println "Working!")
  )

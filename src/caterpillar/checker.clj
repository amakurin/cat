(ns caterpillar.checker
  (:use korma.core)
  (:require
   [clj-http.client :as httpc]
   [caterpillar.processors :as proc]
   [caterpillar.nlp :as nlp]
   [caterpillar.system :as sys]
   [caterpillar.maps :as maps]
   [caterpillar.config :as conf]
   [caterpillar.storage :as store]
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
   [clojure.core.async :refer [chan go >! <!! >!! put! close! thread] :as async]
   ))


(def chans (atom {:input-chan nil}))


(defn link-ok? [url status trace-redirects]
  (let [lrd (last trace-redirects)]
    (and (not= status 404)
         (or (not lrd)
             (and lrd (re-find (re-pattern (s/replace url #"([\.\?\!\-])" #(str "\\" (first %)))) lrd)))))
  )

(defn do-check [{:keys [task-id timeout-ms
                        query-hours
                        storage-entity-src
                        storage-entity-tgt] :as opts}]
  (ti/info "Checker " task-id " handler started...")
  (doseq [{:keys [lid url] :as link}
          (select :pub
                  (fields [:pub.id :lid] [:ads.url :url])
                  (where {:unpub 0
                          :created
                          [> (tf/unparse
                              (tf/with-zone (tf/formatters :mysql) (tc/default-time-zone))
                              (tc/minus (tc/now)(tc/hours query-hours)))]})
                  (join :ads (= :ads.id :id))
                  (order :pub.created :ASC))]
    (err/with-try
     {:link url :step :checker}
     (let [{:keys [status trace-redirects] :as resp} (httpc/head url {:as :auto :throw-exceptions false})]
       (when-not (link-ok? url status trace-redirects)
         (update :pub
                 (set-fields {:unpub 2})
                 (where {:id lid}))
         (ti/info "Checker unpublished id:" lid " url: " url)
         (Thread/sleep timeout-ms))
       (when (not= status 200)
         (ti/info "Checker DANGER STATUS:" status " id:" lid " url: " url )))))
  (ti/info "Checker " task-id " handler procceed."))

;; (let [url "http://m.avito.ru/samara/kvartiry/1-k_kvartira_39_m_12_et._344868371"
;;       {:keys [status trace-redirects] :as resp}
;;       (httpc/head url
;;                   {:as :auto :throw-exceptions false})]
;;   (link-ok? url status trace-redirects)
;;   )



(defn check-loop []
  (thread
   (loop []
     (when-let [opts (<!! (:input-chan @chans))]
       (do-check opts)
       (recur)))
   (println "Check channel closed.")))

(defn
  ^{:system-stop true}
  on-stop []
  (when-let [{:keys [input-chan]} @chans]
    (when input-chan (close! input-chan))))

(defn
  ^{:system-start true}
  on-start []
  (on-stop)
  (reset! chans {:input-chan (chan (async/sliding-buffer 1))})
  (check-loop))

(defn
  ^{:task-handler true}
  task-handler [t opts]
  (>!! (:input-chan @chans) opts))

(def system (sys/subsystem :checker))

;; (sys/init system)
;; (sys/get-config-data system)

;; (sys/start system)
;; (sys/stop system)


;; (task-handler nil
;;  {:storage-entity-src :ads
;;           :storage-entity-tgt :pub
;;   :task-id :x
;;   :sys system})









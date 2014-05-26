(ns caterpillar.publisher
  (:use korma.core)
  (:require
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
   ))


(defn generate-seo-id [rent-variants app-type-str city-str id & [total-area]]
  (let [rent (get rent-variants (rand-int (count rent-variants)))]
    (->
     (str rent " "
          (nlp/naive-word-form (s/replace app-type-str #"\." " ") :accusativus)
          " в "
          (nlp/naive-word-form city-str :oraepositionalis)
          " "
          (when total-area (str "S" total-area))
          " ")
     (tools/ru-translit)
     (str (tools/to-basex (+ 1000000000 id)))
     (s/replace #"\s+" "-"))))

(defn get-district [point city]
  (when point
    (->>
     (:districts city)
     (filter #(maps/inside? point (:poligon %)))
     first)))

(defn get-metro [point city]
  (when point
    (let [{:keys [lat lng] :as metro}
          (->>
           (:metros city)
           (sort-by #(maps/dist-from point [(:lat %)(:lng %)]) <)
           first)]
      (when metro (assoc metro :distance (long (maps/dist-from point [lat lng])))))))

(defn prepare-city [{:keys [mnemo id osm-file-name] :as c}]
  (let [res (proc/to-resource (slurp (str "resources/"osm-file-name)))
        districts (->> (select :districts (where {:city id}))
                       (map (fn [distr]
                              (assoc distr :poligon
                                (maps/get-rel-nodes res (:osm-relation-id distr))))))]
    (merge c {:districts districts
              :metros (->>(select :metros (where {:city id}))
                          (map (fn [{:keys [lat lng]:as m}] (merge m {:lat (Float. lat)
                                                                      :lng (Float. lng)}))))})
    ))

(defn
  ^{:system-merge true}
  load-cities []
  {:cities
   (->>
    (select :cities)
    (map (fn [c] [(:mnemo c) (prepare-city c)]))
    (into {}))})

(defn get-crop [target crops]
  (->> crops
       (map (fn [[k v]] [(re-find k target) v]))
       (remove (fn [[k v]] (nil? k)))
       first second))

(defn create-thumb [link w h]
  (proc/save-image-with-dim link w h))

(defn prepare-imgs [imgs target {:keys [img-crops] :as conf}]
  (let [crop (or (get-crop target img-crops) 0)
        {:keys [width height]} (tools/as-is(env :thumb-size))]
    (err/with-try
     {:link imgs :step :prepare-imgs}
     (->> imgs
          (#(doall (map (fn [img](proc/save-image-with-crop img crop)) %)))
          (remove nil?)
          vec
          ((fn [x] {:imgs (pr-str x)
                    :thumb
                    (when-let [link (first imgs)]
                      (create-thumb link width height))})
           )))))

(defn get-appartment-type [id]
  (when id
    (->>
     (select :appartment-types (fields :name) (where {:id id}))
     first :name)))

(defn prepare-pub [{:keys [id city target extracted-edn] :as ad}
                   {:keys [seo-rent-strings] :as conf}  cities]
  (let [{:keys [appartment-type lat lng ] :as extracted} (read-string extracted-edn)
        point (when (and lat lng) [(Float. lat)(Float. lng)])
        city (get cities city)
        metro (get-metro point city)
        {:keys [imgs thumb]} (prepare-imgs (:imgs extracted) target conf)]
    (-> extracted
        (assoc :id id)
        (assoc :seoid (generate-seo-id
                       seo-rent-strings
                       (get-appartment-type appartment-type)
                       (:name city) id (:total-area extracted)))
        (assoc :district (:id (get-district point city)))
        (assoc :metro (:id metro))
        (assoc :distance (:distance metro))
        (assoc :imgs imgs)
        (assoc :thumb thumb)
        (assoc :city (:id city)))))

 (defn
  ^{:task-handler true}
  task-handler [t {:keys [task-id
                          storage-entity-src
                          storage-entity-tgt
                          sys] :as opts}]
  (let [{:keys [persistent-fields] :as conf} (sys/get-config-data sys)
        cities (sys/get-state sys :cities)]
    (doseq [{:keys [id] :as ad} (select storage-entity-src (where {:published 0 :verdict 0}))]
      (err/with-try
       {:link id :step :publish}
       (let [prepared (prepare-pub ad conf cities)]
         (store/insert-or-update prepared storage-entity-tgt [:id] persistent-fields)
         (update storage-entity-src (set-fields {:published 1}) (where {:id id}))
         )))
    (doseq [{:keys [id] :as ad} (select storage-entity-src (where {:published 1 :verdict [>= 5]}))]
      (err/with-try
       {:link id :step :unpublish}
       (delete storage-entity-tgt (where {:id id}))
       (update storage-entity-src (set-fields {:published 0}) (where {:id id}))))
    (ti/info "Publisher " task-id " handler procceed.")
    ))

(defn util-create-thumbs[]
 (->>
  (select :pub)
  (map #(assoc % :imgs (read-string (:imgs %))))
  (filter #(seq (:imgs %)))
  (#(doall
   (map (fn [x]
          (let [{:keys [imgs id]} x
               {:keys [width height]} (tools/as-is(env :thumb-size))
               thumb (proc/resave-image-with-dim (str (env :crawl-mages) (first imgs)) width height)]
            (println id thumb)
           (update :pub (set-fields {:thumb thumb}) (where {:id id}))
           ))%)))))

;; (util-create-thumbs)

(def system (sys/subsystem :publisher))

;; (sys/init system)
;; (sys/get-config-data system)

;; (sys/start system)
;; (sys/stop system)


;; (task-handler nil
;;  {:storage-entity-src :ads
;;           :storage-entity-tgt :pub
;;   :task-id :x
;;   :sys system})









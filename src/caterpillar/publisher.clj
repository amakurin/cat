(ns caterpillar.publisher
  (:use korma.core)
  (:require
   [caterpillar.processors :as proc]
   [caterpillar.nlp :as nlp]
   [caterpillar.system :as system]
   [caterpillar.maps :as maps]
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


(defn generate-seo-id [rent-variants app-type-str city-str id]
  (let [rent (get rent-variants (rand-int (count rent-variants)))]
    (->
     (str rent " "
          (nlp/naive-word-form (s/replace app-type-str #"\." " ") :accusativus)
          " в "
          (nlp/naive-word-form city-str :oraepositionalis)
          " "
          (tools/to-basex (+ 1000000000 id)))
     (tools/ru-translit)
     (s/replace #"\s+" "-"))))

(defn get-district [point city]
  (->>
   (:districts city)
   (filter #(maps/inside? point (:poligon %)))
   first))

(defn get-metro [point city]
  (let [{:keys [lat lng] :as metro}
        (->>
         (:metros city)
         (sort-by #(maps/dist-from point [(:lat %)(:lng %)]) <)
         first)]
    (when metro (assoc metro :distance (long (maps/dist-from point [lat lng]))))))

(defn
  ^{:task-handler true}
  task-handler [t {:keys [task-id storage-entity] :as opts}]
  (timbre/info "hormiga "))

;; (def sys (system/subsystem :hormiga))

;; (system/start sys)

(defn prepare-city [{:keys [mnemo id osm-file-name] :as c}]
  (let [res (proc/to-resource (slurp (str "resources/"osm-file-name)))
        districts (->> (select :districts (where {:city-id id}))
                       (map (fn [distr]
                              (assoc distr :poligon
                                (maps/get-rel-nodes res (:osm-relation-id distr))))))]
    (merge c {:districts districts
              :metros (->>(select :metros (where {:city-id id}))
                          (map (fn [{:keys [lat lng]:as m}] (merge m {:lat (BigDecimal. lat)
                                                                      :lng (BigDecimal. lng)}))))})
    ))

(defn load-cities []
  (->>
   (select :cities)
   (map (fn [c] [(:mnemo c) (prepare-city c)]))
   (into {})))


(def cities (load-cities))

[53.205617 50.161019]
cities
(get-district [53.205617 50.161019] (get cities ":smr"))
(get-metro [53.205617 50.161019] (get cities ":smr"))

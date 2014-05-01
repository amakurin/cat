(ns caterpillar.maps
  (:use korma.core)
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
   [taoensso.timbre :as ti]
   [clojure.string :as s]
   [clj-time.core :as tc]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   ))


(defn get-osm [res entity & [id child-selector attr-selector]]
  (let [child-selector (if child-selector [child-selector] [])
        attr-selector (if attr-selector [:attrs attr-selector] [:attrs])
        entity-selector (if id [entity (html/attr= :id id)] entity)]
    (->> res
         (proc/select (-> [:osm entity-selector] (concat child-selector) vec))
         (map (fn [child] (get-in child attr-selector))
              ))))

(defn dist-from [p1 p2]
  (let [earth 6371000
        lat1 (first p1) lat2 (first p2)
        lng1 (second p1) lng2 (second p2)
        dlat (Math/toRadians(- lat2 lat1))
        dlng (Math/toRadians(- lng2 lng1))
        a (+ (*(Math/sin(/ dlat 2))
               (Math/sin (/ dlat 2)))
             (* (Math/cos(Math/toRadians lat1))
                (Math/cos (Math/toRadians lat2))
                (Math/sin(/ dlng 2))
                (Math/sin(/ dlng 2))))
        ]
    (* earth 2 (Math/atan2 (Math/sqrt a) (Math/sqrt(- 1 a))))
    ))

(defn glue [ways]
  ;; this one assumes that ways are not perfectly fit each other,
  ;; but i guess they are, if so we can check equality rather then distance
  (loop [w (rest ways) result [(first ways)]]
    (if (seq w)
      (let [lr (last(peek result))
            sdirect
            (sort-by first
                     (fn [x y] (compare (dist-from x lr)
                                        (dist-from y lr))) w)
            sreverse
            (sort-by last
                     (fn [x y] (compare (dist-from x lr)
                                        (dist-from y lr))) w)
            ]
        (if (<= (dist-from (-> sdirect first first) lr)
                (dist-from (-> sreverse first last) lr))
          (recur (rest sdirect) (conj result (first sdirect)))
          (recur (rest sreverse) (conj result (-> sreverse first reverse)))))
      result)))

(defn get-rel-nodes [res rel-id]
  (let [nodes (->> (get-osm res :node )
                   (map (fn[{:keys [id lat lon]}] [id [(BigDecimal. lat) (BigDecimal. lon)]]))
                   (into {}))]
    (->> (get-osm res :relation rel-id [:member (html/attr= :type "way")] :ref)
         (map (fn[x]
                (->>
                 (get-osm res :way x :nd :ref)
                 (map #(get nodes %))
                 )))
         glue
         (mapcat identity)
         )))


;; (time
;; (let [res (proc/to-resource (slurp "resources/samara.xml"))]
;;   (get-district [53.1836645 50.098772] res)
;; ))

(defn- crossing-number
  "Determine crossing number for given point and segment of a polygon.
   See http://geomalgorithms.com/a03-_inclusion.html"
  [[px py] [[x1 y1] [x2 y2]]]
  (if (or (and (<= y1 py) (> y2 py))
          (and (> y1 py) (<= y2 py)))
    (let [vt (/ (- py y1) (- y2 y1))]
      (if (< px (+ x1 (* vt (- x2 x1))))
        1 0))
    0))

(defn inside?
  "Is point inside the given polygon?"
  [point polygon]
  (odd? (reduce + (for [n (range (- (count polygon) 1))]
                    (crossing-number point [(nth polygon n)
                                            (nth polygon (+ n
                                            1))])))))


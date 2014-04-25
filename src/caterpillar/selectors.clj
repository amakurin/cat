(ns caterpillar.selectors
  (:require
   [caterpillar.tools :as tools]
   [net.cgrand.enlive-html :as html]
   ))

(defn
  ^{:accessible-online? true}
  nth-of-type [n]
  (html/nth-of-type n))

(defn
  ^{:accessible-online? true}
  but [selector]
  (html/but selector))

(defmulti read-selector class)

(defmethod read-selector clojure.lang.PersistentVector
  [selector]
  (loop [s selector result []]
    (if-let [cur (first s)]
      (recur (rest s) (conj result (read-selector cur)))
      result)))

(defmethod read-selector clojure.lang.PersistentArrayMap
  [selector] ;; todo refactor with same functionality of processors
  (if-let [info (-> selector :func ((tools/list-functions 'caterpillar.selectors)))]
    (apply (:fn-self info) (->> info
                                :arglists
                                first
                                (map #((keyword %) (:args selector)))))
           selector))

(defmethod read-selector :default
  [selector]  selector)


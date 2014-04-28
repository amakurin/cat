(ns caterpillar.formatters
  (:require
   [caterpillar.tools :as tools]
   [caterpillar.processors :as proc]
   [clojure.string :as s]
   ))

(defn
  ^{:accessible-online? true}
  area [x]
  (->> x
       (filter #(and % (nil?(re-find #"[^\d\.\,]" %))))
       (map #(BigDecimal. (s/replace % #"\," "." )))
       (sort >)
       vec))

(defn
  ^{:accessible-online? true}
  money [x]
  (let [n (second x)
        t (nth x 2)
        tw (nth x 3)
        fraction? (re-find #"\D" n)]

    (cond fraction?
          (long (* 1000 (BigDecimal. (s/replace n #"\," "." ))))
          t
          (Integer. (s/join [n t]))
          tw
          (*(Integer. n)1000)
          :else
          (Integer. n))))

(defn
  ^{:accessible-online? true}
  phone [x]
  (->> x
       first
       proc/get-phone))

(defn
  ^{:accessible-online? true}
  floor [x]
  (->> x
       rest
       (filter #(and % (nil?(re-find #"\D" %))))
       (map #(Integer. %))
       (sort <)
       vec))

(defn
  ^{:accessible-online? true}
  dictionary [x]
  (->> (map (fn [x r] [r x]) (rest x)(range))
       (filter second)
       first first))

(defn
  ^{:accessible-online? true}
  just-match [x]
  (->> x
       first))

(defmulti read-formatter class)

(defmethod read-formatter clojure.lang.PersistentVector
  [formatter]
  (loop [f formatter result []]
    (if-let [cur (first f)]
      (recur (rest f) (conj result (read-formatter cur)))
      (->> result reverse (apply comp)))))

(defmethod read-formatter clojure.lang.PersistentArrayMap
  [formatter] ;; todo refactor with same functionality of processors
  (-> formatter :func ((tools/list-functions 'caterpillar.formatters)) :fn-self))

(defmethod read-formatter nil
  [formatter]  (fn[_] true))

(defmethod read-formatter :default
  [formatter]  formatter)

(defn do-format [formatter v]
  ((read-formatter formatter) v))



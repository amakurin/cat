(ns caterpillar.tools
  (:require
   [clojure.string :as s]
   [clj-http.client :as httpc]
   )
  (:import
   java.io.StringReader
   java.net.URL
   (clojure.lang IDeref ISeq IPersistentMap IPersistentSet IPersistentCollection)
   java.util.concurrent.Executors
   ))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn gen-file-name [ext]
  (str (s/replace (uuid) #"-" "") "." (name ext)))

(defn valid-url? [src]
  (try
    (URL. src) src
    (catch java.net.MalformedURLException e nil)))

(defn base-url [url-str]
  (try
    (let [url (URL. url-str)]
      (str  (.getProtocol url) "://" (.getHost url)))
    (catch java.net.MalformedURLException e nil)))

(defn prepend-base-url [url base]
  (cond (base-url url) url
        (re-find #"^//" url) (str (.getProtocol (URL. base)) ":" url)
        :else (str base url)))

(defn get-funcs [ns metakey]
  (->> (ns-publics (symbol ns))
       (filter (fn [[k v]] (metakey (meta v))))
       (map (fn [[k v]]
              (let [{:keys [doc arglists]} (meta v)]
                [(keyword k)
                 {:fn-self v
                  :doc doc
                  :arglists arglists
                  }])))
       (into {})))

(defn list-functions [ns]
  (get-funcs ns :accessible-online?))

(defn ^{:dont-test "Used in impl of thread-local"}
  thread-local*
  "Non-macro version of thread-local - see documentation for same."
  [init]
  (let [generator (proxy [ThreadLocal] []
                    (initialValue [] (init)))]
    (reify IDeref
      (deref [this]
        (.get generator)))))

(defmacro thread-local
  "Takes a body of expressions, and returns a java.lang.ThreadLocal object.
   (see http://download.oracle.com/javase/6/docs/api/java/lang/ThreadLocal.html).

   To get the current value of the thread-local binding, you must deref (@) the
   thread-local object. The body of expressions will be executed once per thread
   and future derefs will be cached.

   Note that while nothing is preventing you from passing these objects around
   to other threads (once you deref the thread-local, the resulting object knows
   nothing about threads), you will of course lose some of the benefit of having
   thread-local objects."
  [& body]
  `(thread-local* (fn [] ~@body)))

(defn get-urls [url url-param]
  (cond (and url-param (set? url-param))
        (map #(format url (str %)) url-param)
        (and url-param (vector? url-param))
        (map #(format url (str %)) (range (first url-param)(second url-param)))
        :else url))

(defn get-page [uri]
;  (try
    (httpc/get uri)
;    (catch Exception e {:error e}))
  )

(defn bool-to-binary [v] (if v 1 0))

(defn deep-merge-with
  [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))


(defn map-or-entry [x]
  (cond
   (map? x) (map identity x)
   (or (coll? x) (= 2 (count x))) [(vec x)]
   :else (throw (Exception. "Only map or collection of size 2 supported by into-map."))))

(defn logical-empty?[x]
  (cond
   (string? x) (-> x (s/replace #"^[\s\n]+|[\s\n]+$" "") empty?)
   (or (seq? x) (coll? x)) (empty? x)
   (number? x) (= 0 x)
   (nil? x) true
   :else false))

(defn map-reducer [x [k v]]
  (if (logical-empty? v) x
    (let [exv (get x k)]
      (assoc x k
         (cond (nil? exv) v
               (and (coll? exv) (coll? v))
               (distinct (concat exv v))
               (and (not (coll? exv))(coll? v))
               (distinct (conj v exv))
               (and (not (coll? v))(coll? exv))
               (distinct (conj exv v))
               (and (not (coll? v))(not (coll? exv)))
               (if (= v exv) exv [v exv])
               )))))

(defn reduce-to-map [from]
  (->> from
  (mapcat map-or-entry)
  (reduce map-reducer {})))


(defn test-stm [nthreads niters task]
  (let [pool  (Executors/newFixedThreadPool nthreads)
        tasks (map (fn [t]
                      (fn []
                        (dotimes [n niters]
                          (task)
                           )))
                   (range nthreads))]
    (doseq [future (.invokeAll pool tasks)]
      (.get future))
    (.shutdown pool)))


(defn cartesian [colls]
  (if (empty? colls)
    '(())
    (for [x (first colls)
          more (cartesian (rest colls))]
      (cons x more))))

(defn do-split-by [data fields]
  (if (seq fields)
    (->> fields
         (select-keys data)
         (filter #(-> % second coll?))
         (map #(partition 2 (interleave (repeat (first %))(second %))))
         cartesian
         (map #(->> % (map vec) (into {}) (merge data))))
    [data]))

(defn do-filter-by [data filt]
  (if (and (map? filt) (seq filt))
    (filter (fn [item] (every? (fn [[fk fv]] (= fv (fk item))) filt))data)
    data))

(defn ru-translit [st]
  (let [alphabet
        {\р "r", \с "s", \т "t", \у "u", \ф "f", \х "h", \ц "c", \ч "ch", \ш "sh", \щ "sh", \ъ "'",
         \ы "y", \ь "'", \э "e", \ю "u", \я "ya", \а "a", \б "b", \ё "e", \в "v", \г "g", \д "d",
         \е "e", \ж "zh", \з "z", \и "i", \й "i", \к "k", \л "l", \м "m", \н "n", \о "o", \п "p"}]
    (->> st s/lower-case (map #(if-let [letter (get alphabet %)] letter %)) (apply str))))

(defn to-basex [value]
  (let [dict [\B \C \D \F \G \H \J \K \L \M \N \P \Q \R \S \T
              \V \W \X \Y \Z \b \c \d \f \g \h \j \k \l \m \n \p \q \r \s \t \v \w \x \y \z]
        base (count dict)]
    (if (= 0 value) "0"
      (loop [remaining value exponent 1 result []]
        (if (= 0 remaining)
          (->> result reverse (apply str))
          (let [a (long (Math/pow base exponent))
                b (mod remaining a)
                c (Math/pow base (dec exponent))
                d (/ b c)]
            (recur (- remaining b) (inc exponent) (conj result (get dict (long d))))
            ))))))


;; todo replace locks with something more clojurish,
;; sync agents maybe
(defprotocol ILocks
  (get-lock [_ k]))

(defn lock-provider []
  (let [locks (atom {})]
    (reify
      ILocks
      (get-lock [_ k] (or (k @locks) (k (swap! locks (fn [l] (if (k l) l (assoc l k (Object.))))))))
      )))

(defn as-is [v]
  (if (string? v) (read-string v) v))


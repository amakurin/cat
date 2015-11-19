(ns caterpillar.processors
  (:require
   [caterpillar.selectors :as sel]
   [caterpillar.tools :as tools]
   [caterpillar.ocr :as ocr]
   [caterpillar.nlp :as nlp]
   [net.cgrand.enlive-html :as html]
   [net.cgrand.jsoup :as jsoup]
   [clojure.string :as s]
   [clj-http.client :as httpc]
   [clj-time.core :as tc]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [clojure.java.io :as io]
   [environ.core :refer [env]]
   )
  (:import
   java.io.StringReader
   java.io.ByteArrayInputStream
   java.net.URL
   [org.apache.commons.codec.binary Base64 Base32 Hex]))

(def local-context (tools/thread-local (atom {})))

(defn create-context [context]
  (reset! @local-context context))

(defn merge-context [context]
  (swap! @local-context merge context))

(defn get-in-local-context[path]
  (get-in @@local-context path))

(defn set-current! [k v]
  (merge-context {k v}) v)

(defn set-current-url! [v]
  (set-current! :url v))

(defn current-url []
  (get-in-local-context [:url]))

(defn current-proxy []
  (get-in-local-context [:prox]))

(defn get-arg [arglist-entry]
   (cond (map? arglist-entry) {:arg (:as arglist-entry)}
         (vector? arglist-entry) (map (fn [x] {:arg x :opt? true}) arglist-entry)
         (= '& arglist-entry) nil
         :else {:arg arglist-entry}))

(defn get-args [arglist]
  (->> arglist (map get-arg) flatten (remove nil?)))

(defn resolve-args [args data]
  (let [{:keys [conf] :as context} @@local-context]
    (if-not args {}
      (->> args
           (map (fn [[k v]]
                  [k (cond (and (vector? v) (->> v first (= :conf)))
                           (get-in conf (rest v))
                           (and (vector? v) (->> v first (= :data)))
                           (get-in data (-> v rest vec))
                           (and (vector? v) (->> v first (= :local-context)))
                           (get-in context (rest v))
                           :else v)]))
           (into {})))))

(defn argfunc[func ns]
  (->> (func (tools/list-functions ns))
             :arglists first get-args))

(defn prepare-arg [{:keys [arg opt?] :as arg} resolved data]
  (let [arg-key (keyword arg)
        arg-val (arg-key resolved)]
    (cond (or arg-val (contains? resolved arg-key)) {:value arg-val}
          opt? {:remove? true}
          :else {:value data :auto-data? true})))

(defn prepare-func [{:keys [func args] :as f}]
  (let [info (func (tools/list-functions 'caterpillar.processors))
        arg-list (->> info :arglists first get-args)]
    (fn [data]
      (let [resolved (resolve-args args data)]
        (apply (:fn-self info)(->> arg-list
                                  (map #(prepare-arg % resolved data))
                                  (remove :remove?)
                                  (map :value)))))))

(defn trim-if-string [v]
  (if (string? v) (s/trim v) v))

(defn compose [funcs]
  (apply comp (conj (->> funcs (map prepare-func) reverse vec)trim-if-string)))

;;;;;;;;Actually processors

(defn do-http-get [url opts]
  (let [{:keys [proxy-host proxy-port proxy-creds] :as prox} (current-proxy)
        proxy-opts (when (and prox proxy-host) (select-keys prox [:proxy-host :proxy-port]))
        {:keys [headers] :as opts} (if proxy-opts (merge opts proxy-opts {:insecure? true}) opts)
        opts (assoc opts :headers (merge headers
                                         {"User-Agent" "Mozilla/5.0 (Windows NT 6.1;) Gecko/20100101 Firefox/13.0.1"}))]
    (httpc/get url opts)))

 #_(let [cnt (create-context {:prox {:proxy-host "78.46.210.21"
   :proxy-port 3128
   :proxy-creds "caterpillarrobot:123QWEas"
   }})]
   (current-proxy)
 (do-http-get
  "https://m.avito.ru/samara/komnaty/sdam/na_dlitelnyy_srok?page=1"
  {:as :auto}
  )
   )

#_(httpc/head
    "http://m.avito.ru/samara/komnaty/sdam/na_dlitelnyy_srok?page=1"
    {:headers
     {"User-Agent" "Mozilla/5.0 (Windows NT 6.1;) Gecko/20100101 Firefox/13.0.1"
      }
     })

;; (httpc/get "https://floor16.ru"
;;            {:as :auto
;;             :proxy-host "78.46.210.21"
;;             :proxy-port 3128
;;             :insecure? true
;;             })

(defn
  ^{:accessible-online? true}
  to-resource [src]
  (let [prox (current-proxy)
        src (if (tools/valid-url? src)
              (:body (do-http-get src {:as :auto}))
              src)]
    ;(html/html-resource (StringReader. src))
    (html/html-resource (ByteArrayInputStream. (.getBytes src "UTF-8")) {:parser jsoup/parser})
    ))

(defn
  ^{:accessible-online? true}
  select [selector resource]
  (html/select resource (sel/read-selector selector)))

(defn
  ^{:accessible-online? true}
  prepend-base-url [url base-source]
  (let [base (tools/base-url base-source)]
    (tools/prepend-base-url url base)))

(defn
  ^{:accessible-online? true}
  get-attribute [{:keys [tag attrs] :as resource} attr]
  (let [auto-prepend {:a :href :img :src :span :data-img-src}
        value (attr attrs)]
    (if (= (auto-prepend tag) attr)
      (prepend-base-url value (current-url))
      value)))

(defn
  ^{:accessible-online? true}
  set-value [value] value)

(defn
  ^{:accessible-online? true}
  str-format [resource fstr]
  (format fstr (str resource)))

(defn
  ^{:doc
    "Base64 decode
    raw-value: string"
    :accessible-online? true}
  decode-base64 [resource]
  (String. (Base64/decodeBase64 resource)))

(defn
  ^{:accessible-online? true}
  recognize-image-text [url]
  (ocr/recognize {:src-url url}))

(defn
  ^{:accessible-online? true}
  save-image-with-crop [url bottom-crop]
  (try
    (if-let [buf (ocr/read-image-with-crop (URL. url) bottom-crop (or (tools/as-is(env :crawl-mages-min-size)) 100))]
      (let [path (or (env :crawl-mages) "resources/img/")
            file-name (str (s/replace (tools/uuid) #"\-" "") ".jpg")]
        (ocr/buf-to-file
         {:buf buf
          :path (str path file-name)
          :format :jpg
          })
        file-name)
      nil)
    (catch Exception e nil)))

(defn
  ^{:accessible-online? true}
  save-image-with-dim [url w h]
  (try
    (if-let [buf (ocr/read-image-dim (URL. url) w h)]
      (let [path (or (env :crawl-mages) "resources/img/")
            file-name (str (s/replace (tools/uuid) #"\-" "") ".jpg")]
        (ocr/buf-to-file
         {:buf buf
          :path (str path file-name)
          :format :jpg
          })
        file-name)
      nil)
    (catch Exception e nil)))

(defn
  resave-image-with-dim [img-path w h]
  (try
    (if-let [buf (ocr/read-image-dim (io/file img-path) w h)]
      (let [path (or (env :crawl-mages) "resources/img/")
            file-name (str (s/replace (tools/uuid) #"\-" "") ".jpg")]
        (ocr/buf-to-file
         {:buf buf
          :path (str path file-name)
          :format :jpg
          })
        file-name)
      nil)
    (catch Exception e nil)))

(defn
  ^{:accessible-online? true}
  zipmap-with [resource labels]
  (zipmap labels resource))

(defn
  ^{:accessible-online? true}
  apply-concat [resource]
  (if (coll? resource)
    (mapcat #(if (coll? %) % [%]) resource)
    resource))

(defn
  ^{:doc
    "Extract features by applying feature semantics, return map of feature-key to bool
    raw-value: string
    features: map of feature-key to vector of strings"
    :accessible-online? true}
  extract-features [resource features]
  (->> features
       (map (fn [[k dict]] [k (nlp/in-semantics? resource dict)]))
       (into {})))
(defn
  ^{:doc
    "Remove all non-digits and cast result to int
    raw-value: string"
    :accessible-online? true}
  as-integer [resource]
  (-> resource
      (s/replace #"\D" "")
      (#(if (empty? %) nil (Integer. %)))))

(defmulti deep-as-text class)

(defmethod deep-as-text clojure.lang.LazySeq
  [resource]
  (loop [inp resource res ""]
    (if-let [cur (first inp)]
      (recur (rest inp) (str res (deep-as-text cur)))
      res)))

(defmethod deep-as-text clojure.lang.PersistentArrayMap
  [resource] (deep-as-text (:content resource)))

(defmethod deep-as-text java.lang.String
  [resource]
  (let [s (s/replace resource #"^[\s\n\t]+$" "")]
    (if (seq s)
      (str " " s) "")))

(defmethod deep-as-text :default
  [resource] "")

(defn
  ^{:accessible-online? true}
  as-text [resource]
  (-> (deep-as-text resource)
      (s/replace #"^[\s\n]+|[\s\n]+$" "")
      (s/replace #"\t+" " ")
      ))

(defn
  ^{:doc
    "Find all digit groups and produce list of int
    raw-value: string"
    :accessible-online? true}
  get-integers [resource]
  (->> resource
       (re-seq #"\d+")
       (map #(Integer. %))))

(defn trim-left-to-count [s & [cnt]]
  (let [cnt (or cnt 10)]
    (if (> (count s) cnt)
      (subs s 1)
      s)))

(defn
  ^{:doc
    "Remove all non-digits and trim-left to length of 10
    raw-value: string"
    :accessible-online? true}
  get-phone [resource]
  (-> resource
      (s/replace #"\D" "")
      trim-left-to-count))

(defn
  ^{:doc
    "Extract phones from string
    raw-value: string"
    :accessible-online? true}
  get-phones-spec [resource]
  (->> resource
      (#(s/replace % #"\s|\-|\.|\\|/|\*|\`|\'|\"|\+" ""))
      (re-seq #"[78]??\(?9\d{2}\)?\d{7}|\d{7}")
      (map get-phone)))

(defn
  ^{:doc
    "Extract phones from string
    raw-value: string"
    :accessible-online? true}
  get-phones [resource]
  (->> resource
       (re-seq
        #"(?iux)
        (?<![\d])
        (?: \b(?: т|тел)[\.\:]?\s?)?
        (?: \+\s?)?
        (?: [78][\.\-\s]?)?
        (?: \(\d{3}\)\s?|(?: \d\s?[\.\-]?\s?){3})
        (?: (?: \d\s?[\.\-\s]?\s?){6,7}|(?: \d\s?[\.\-\s]?\s?){4})
        \b(?![\d])"
        )
       (map get-phone)
       ))

(defn
  ^{:doc
    "Extract areas from string
    raw-value: string"
    :accessible-online? true}
  get-areas-from-text [resource]
  (->> resource
       (re-seq
        #"((\d{1,3}[\.\,]?\d?)[\s\\/\-]+)?((\d{1,3}[\.\,]?\d?)[\s\\/\-]+)?(\d{1,3}[\.\,]?\d?)\s*(кв\.?\s*м|м\s*[\s\.2]|метр)"
        )
       (apply concat)
       (filter #(and % (nil?(re-find #"[^\d\.\,]" %))))
       (map #(BigDecimal. (s/replace % #"\," "." )))
       (sort >)
       ))

(defn
  ^{:doc
    "Extract floors from string
    raw-value: string"
    :accessible-online? true}
  get-floors-from-text [resource]
  (->> resource
      (re-seq #"((\d{1,2})[\s\\/\-]+)?(\d{1,2})\s*\-?\s*эт")
      (apply concat)
      (filter #(and % (nil?(re-find #"\D" %))))
      (map #(Integer. %))
      (sort <)
      ))

(defn
  ^{:doc
    "Trim and lower-case, then finds it in dict
    raw-value: string
    dict: map with string keys"
    :accessible-online? true}
  get-key [resource dict]
  (get dict (-> resource s/trim s/lower-case)))

(defn
  ^{:doc
    "Find pattern, if found key-true, else key-false
    raw-value: string
    pattern: string
    key-true: key-false: any
    dict: map with string keys"
    :accessible-online? true}
  key-by-occur [resource pattern key-true key-false]
  (if (re-find (re-pattern pattern) (s/lower-case resource)) key-true key-false))


(defn
  ^{:doc
    "Find pattern, if found key-true, else key-false
    raw-value: string
    pattern: string
    key-true: key-false: any
    dict: map with string keys"
    :accessible-online? true}
  get-person-type-from-text [resource]
  (key-by-occur
   resource
    #"услуги\s*[\-\:\,\.]?по\s*\-?\s*факту|[\d%]\s*[\-\:\,\.]?по\s*\-?\s*факту|по\s*\-?\s*факту\s*(вс|за)|р[ие][эе]лторски[ехм]|агент?ски[ехм]|низкая комиссия|пр[ие]соед[ие]няй|пр[ие]обретай|(комиссия|агент[ау]|агент?ств[оау]|р[ие][эе]лтор[ау])\s*[\:\-\,\.]?\s*\d{1,3}"
   :agent
   :owner
   ))

;(get-person-type-from-text
;" Хочешь снять такую квартиру без посредников и риелторов?Есть газета «Аренда квартир в Самаре без посредников». Все проверенные объявления только там!Уже сотни людей нашли себе квартиру без риелторов и агентов! Присоединяйтесь и Вы к ним.Приобрести газету можно в фирменном киоске внутри станции метро Спортивная.Свежий номер каждый понедельник, среду и пятницу.Ближайший свежий номер газеты приобретайте 16 Апреля 2014 г.Телефон для связи 2-723-733, остались вопросы звоните, все расскажем! "
; )

(defn
  ^{:doc
    "Find all words (cyr, lat and numeric) as list
    raw-value: string"
    :accessible-online? true}
  get-words [resource]
  (->> resource
       (re-seq #"\p{IsCyrillic}+|\w+")))

(defn
  ^{:doc
    "Gets only word part of string from begining
    raw-value: string"
    :accessible-online? true}
  text-while-words [resource]
  (->> resource
       (re-find #"[\p{IsCyrillic}|\w|\s]+")
       (#(if (nil? %) nil (s/trim %)))))

(defn
  ^{:doc
    "Find match by pattern
    raw-value: string"
    :accessible-online? true}
  find-pattern [resource pattern]
  (let [pattern (if (string? pattern) (re-pattern pattern) pattern)
        found (re-find pattern resource)]
    (if (vector? found) (last found) found)))


;;;;datetime
(defn try-date-pattern [s {:keys [pattern formatter]}]
  (let [found (find-pattern s pattern)]
    (when found
      (try
        (cond (string? formatter)
              (tf/parse (tf/formatter formatter) found)
              (instance? org.joda.time.format.DateTimeFormatter formatter)
              (tf/parse formatter found)
              (fn? formatter)
              (formatter found)
              :else (throw (Exception. "Unknown date formatter type.")))
        (catch java.lang.IllegalArgumentException e nil)))))

(defn today-yester [s & [now]]
  (let [today (or now (tc/today))
        s (-> s s/trim s/lower-case)]
    (cond
     (or (= s "сегодня")(= s "today")) today
     (or (= s "вчера")(= s "yesterday"))
     (tc/minus today (tc/days 1)))))

(defn extract-date[resource & [now]]
  (let [today (or now (tc/today))
        patterns
        [{:pattern #"\d{2}\.\d{2}\.\d{4}"
          :formatter "dd.MM.yyyy"}
         {:pattern #"\d{2}\s[\p{IsCyrillic}|\w|]{3}\.\s\d{4}"
          :formatter "dd MMM. yyyy"}
         {:pattern #"\d{1}\s[\p{IsCyrillic}|\w|]{3}\.\s\d{4}"
          :formatter "d MMM. yyyy"}
         {:pattern #"\d{2}\s[\p{IsCyrillic}|\w|]{4,}\s\d{4}"
          :formatter "dd MMMM yyyy"}
         {:pattern #"\d{1}\s[\p{IsCyrillic}|\w|]{4,}\s\d{4}"
          :formatter "d MMMM yyyy"}
         {:pattern #"(\d{2}\s[\p{IsCyrillic}|\w|]{4,})[\s|\.|,]"
          :formatter (.withDefaultYear (tf/formatter "dd MMMM") (-> today tc/year))}
         {:pattern #"(\d{1}\s[\p{IsCyrillic}|\w|]{4,})[\s|\.|,]"
          :formatter (.withDefaultYear (tf/formatter "d MMMM") (-> today tc/year))}
         {:pattern #"\d{2}\s[\p{IsCyrillic}|\w|]{3}\."
          :formatter (.withDefaultYear (tf/formatter "dd MMM.") (-> today tc/year))}
         {:pattern #"\d{1}\s[\p{IsCyrillic}|\w|]{3}\."
          :formatter (.withDefaultYear (tf/formatter "d MMM.") (-> today tc/year))}
         {:pattern #"\d{2}\s[\p{IsCyrillic}|\w|]{3}"
          :formatter (.withDefaultYear (tf/formatter "dd MMM") (-> today tc/year))}
         {:pattern #"\d{1}\s[\p{IsCyrillic}|\w|]{3}"
          :formatter (.withDefaultYear (tf/formatter "d MMM") (-> today tc/year))}
         {:pattern #"([\p{IsCyrillic}|\w|]+)[\s|\.|,].*\d{2}:\d{2}"
          :formatter #(today-yester % today)}]
        d (->> resource
               (#(map (fn [p] (try-date-pattern % p)) patterns))
               (some identity))]
    (or d today)))

(defn extract-time[resource & [now]]
  (let [now (or now (tl/local-now))
        found (find-pattern resource #"\d{2}:\d{2}")]
    (if found
      (try
        (tf/parse (tf/formatter "HH:mm") found)
        (catch java.lang.IllegalArgumentException e now))
      now)))

(defn
  ^{:accessible-online? true}
  get-date-time [resource & [now]]
  (let [now (or now (tl/local-now))
        d (extract-date resource now)
        t (extract-time resource now)
        found (tc/from-time-zone
               (tc/date-time (tc/year d)(tc/month d)(tc/day d)(tc/hour t)(tc/minute t))
               (tc/default-time-zone))]
    (tf/unparse (tf/formatters :mysql) (if (tc/after? found (tl/local-now))(tc/minus found (tc/days 1)) found))))
;;;;end of datetime

(defn
  ^{:accessible-online? true}
  extract-field [{:keys [id selector processors collection?] :as field-meta} resource & [flat]]
  (let [resource (if selector (select selector resource) [resource])
        resource (if processors (map #((compose processors) %) resource) resource)
        resource (if collection? resource (first resource))
        resource (if (or flat (map? resource)) resource {id resource})
        ]
       resource))

(defn
  ^{:accessible-online? true}
  extract [fields-meta resource]
  (->> fields-meta
       (map #(extract-field % resource))
       tools/reduce-to-map
       ))

;;;; Root processors

(defn
  ^{:accessible-online? true}
  direct-crawl-json [url fields-meta & [headers]]
  (let [headers (or headers {"X-Requested-With" "XMLHttpRequest"})
        headers (assoc headers "Referer" (current-url))]
    (get-in (do-http-get url {:as :json :headers headers})
            [:body])))

(defn
  ^{:accessible-online? true}
  direct-crawl-list [url url-param selector fields-meta]
  (->> (tools/get-urls url url-param)
       (map #(->> %
                  set-current-url!
                  to-resource
                  (select selector)
                  (map (fn [x] (extract fields-meta x)))))
       (apply concat)))

(defn
  ^{:accessible-online? true}
  direct-crawl-item[resource url fields-meta]
  (->> url
       set-current-url!
       to-resource
       (#(extract fields-meta %))
       (merge resource)))

(defn process-target [target data & [conf prox]]
  (let [context (if conf
                  (create-context {:conf conf :target target :initial-data data :prox prox})
                  (merge-context {:target target :initial-data data}))
        composed (compose (get-in context [:conf :targets target]))]
    (composed data)))

(defn
  ^{:accessible-online? true}
  scrape-items [resource & [n]]
  (map #(process-target (:target %) %)
       (if n (take n resource) resource)))



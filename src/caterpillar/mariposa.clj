(ns caterpillar.mariposa
  (:use korma.core)
  (:require
   [caterpillar.formatters :as form]
   [caterpillar.config :as conf]
   [caterpillar.storage :as store]
   [caterpillar.tools :as tools]
   [environ.core :refer [env]]
   [cronj.core :as sched]
   [cronj.data.scheduler :as ts]
   [taoensso.timbre :as timbre]
   [clojure.string :as s]
   ))

;; (re-seq
;; #"(?iux)\b(?:страховой|страх\.?|авансовый)?\s?(?:депозит|взнос|залог)
;;      |посл\.?(?:ед\.?)?(?:ни[йе])?\s?мес\.?(?:[яе]ц)?ы?\b"

;;  "рвый месяц + страховой депозит 19 000 (Собственник)"
;;  )

(defn new-entity [{:keys [t v o p] :or {p 1.} :as entity}]
  [t v o p])

(defn resolve-entity [[t v o p]]
  {:t t :v v :o o :p p})

(defn merge-entity [entity m]
  (new-entity (merge (resolve-entity entity) m)))

(defn clean-text [text]
  (if text
    (-> text
        (s/replace #"^[\s\n]+|[\s\n]+$" "")
        (s/replace #"\.?\s*\n" ". ")
        (s/replace #"\n+" " ")
        (s/replace #"\s+" " ")
        (s/replace #"\u00A0" " ")) ;for case of &nbsp
    ""))

(defn collect-with[{:keys [term gterm pattern formatters] :as meta-entity} text]
  (let [matcher (re-matcher pattern text)]
    (loop [collected []]
      (if-let [match (re-find matcher)]
        (recur (conj collected (merge (dissoc meta-entity :pattern :formatters)
                                      {:match (if (string? match) [match] match)
                                       :value (form/do-format formatters (if (string? match) [match] match))
                                       :start (.start matcher)
                                       :end (.end matcher)})))
        collected))))

(defn match-to-entity [match]
  (new-entity {:t (:term match)
               :v (:value match)
               :o (-> match :match first)}))

(defn clasterize-text[terms text]
  (let [all (->> terms
                 (reduce
                  (fn [matches meta-entity]
                    (concat matches (collect-with meta-entity text)))
                  [])
                 (sort #(if (= (:start %1)(:start %2))
                          (compare (:end %1)(:end %2))
                          (compare (:start %1)(:start %2))
                          ))
                 vec)]
    (loop [ms all txt text result []]
      (if (seq ms)
        (let [end (peek ms)
              with-overlaps (->> ms pop
                                 (filter #(> (:end %) (:start end)))
                                 (map #(assoc % :str (str "{" (match-to-entity %) "}")))
                                 vec
                                 (#(conj % (assoc end :str (str "{" (match-to-entity end) "}")))))
              start (first with-overlaps)]
          (recur (->> ms pop (remove #(> (:end %) (:start end))) vec)
                 (str (subs txt 0 (:start start))
                      (apply str (map :str with-overlaps))
                      (subs txt (:end end))) result))
        txt))))

(defn parse-text [text]
  (->> (re-seq #"\{\[\:[^\{\}\:]+\]\}|[\p{IsCyrillic}\w]+|[\+\.,]" text)
            (map (fn [x]
                   ;(first
                   (if (re-find #"^\{\[" x)
                     (read-string(s/replace x #"^\{|\}$" ""))
                     (cond (or(= x ",")(= x "и"))[:and true x]
                           (= x "c")[:with true x]
                           (= x ".")[:terminate true x]
                           ;(= x ",")[:comma true x]
                           (or (= x "+")(= x "плюс"))[:plus true x]
                           :else [:un nil x])
                     )
                   ;)
                   ))))

(defn make-sub [sub inp]
  (let [inp (->> inp (remove #(= :semicolon (:gterm %))) vec)]
    (cond (= :terminator sub){:gterm sub :v nil}
          (and (= 1 (count inp)) (= sub (->> inp first :gterm)))
          (first inp)
          :else {:gterm sub :v inp})))

(defn substitute [automata input]
  (loop [inp input nonsub []]
    (if (empty? inp)
      nonsub
      (if-let [sub (->> inp (map :gterm) vec (get automata))]
        (recur (conj nonsub (make-sub sub inp)) [])
        (recur (-> inp rest vec) (conj nonsub (first inp)))
        ))))

(defn terminate [grammar input]
  (let [automata
        (->> grammar
             (mapcat (fn[[k v]] (map (fn [x] [x k]) v)))
             (into {})
             )]
    (loop [inp input memory [] output [] cnt 0]
      (if (or (empty? inp) (> cnt 1000))
        memory
        (let [v (first inp)
              gterm (get automata (-> v resolve-entity :t))
              memory (substitute automata (conj memory {:gterm gterm :v v}))
              ]
          ;(println cnt gterm)
          (recur (rest inp) memory output (inc cnt))
          )
        )
      )))

(defn flatten-formula [formula]
  (loop [f formula]
    (if (->> f :v (filter #(= (:gterm %) :formula)) empty?)
      f
      (recur (assoc f :v (vec (mapcat #(if (= (:gterm %) :formula) (:v %) [%]) (:v f)))))
      )))


(defn split-formula[formula]
  (let [prior {:operator 1 :bi-operator 2 :object nil}
        opers (set(map key prior))]
    (loop [inp (:v formula) stack [] out []]
      (if (empty? inp)
        (map #(assoc formula :v %)out)
        (let [current (first inp)
              gcurr (:gterm current)
              fstack (->> stack first :gterm)
              inp (rest inp)
              oper? (opers gcurr)]
          (cond oper?
                (recur inp (cons current stack) out)
                (and fstack (nil? (fstack prior)))
                (recur inp (rest stack) (conj out (->> (cons current stack) (remove #(= :bi-operator (:gterm %))) reverse vec)))
                :else
                (recur inp stack (conj out (->> (cons current stack) (remove #(= :bi-operator (:gterm %))) reverse vec)))
                )
          )))))

(defn simplify-formula [formula]
  (let [prior {:operator 1 :bi-operator 2 :object nil}
        opers (set(map key prior))]
    (loop [inp (:v formula) stack [] out []]
      (if (empty? inp)
        (assoc formula :v (->> (concat out stack) reverse vec))
        (let [current (first inp)
              gcurr (:gterm current)
              inp (rest inp)
              fstack (->> stack first :gterm)
              oper? (opers gcurr)]
          (cond
           (and oper? (nil? (gcurr prior)))
           (recur inp stack (cons current out))
           (and oper? (= fstack gcurr))
           (recur inp stack out)
           (and oper? (or (not fstack) (>= (gcurr prior) (fstack prior))))
           (recur inp (cons current stack) out)
           (and oper? (< (gcurr prior) (fstack prior)) (some #(= (-> % :v resolve-entity :t)
                                                                 (-> current :v resolve-entity :t)) stack))
           (recur inp stack out)
           (and oper? (< (gcurr prior) (fstack prior)))
           (recur inp stack (cons current out))
           :else
           (recur inp stack (cons current out)))
          )))))

(defn get-rule [rules selector]
  (loop [s selector]
    (let [match (get rules (vec s))]
      (if (or match (empty? s))
        match
        (recur (butlast s))
        ))))

(defn prob-by-size[entity-v supposed-size]
  (let [actual-size (if (coll? entity-v) (count entity-v) 1)]
    (cond (number? supposed-size)
          (* 1. (/ (min supposed-size actual-size)(max supposed-size actual-size)))
          (map? supposed-size)
          (or (get supposed-size actual-size) 0)
          :else 1.)))

(defn prob-by-border [pred]
  (fn [entity-v [border p]]
    (let [entity-v (if (coll? entity-v) (first entity-v) entity-v)]
      (cond (not (number? entity-v)) 1
            (pred entity-v border) p
            :else (- 1 p)
            ))))

(defn get-prob [entity rule]
  (let [{:keys [t v o p]} (resolve-entity entity)
        probs {:prob-by-size prob-by-size :prob-by-lowest (prob-by-border <):prob-by-highest (prob-by-border >)}]
    (->> probs
         (map (fn [[k pred]]
           (let [i (.indexOf rule k)]
             (if (= i -1) nil (pred v (get rule (inc i)))))))
         (filter identity)
         (apply * p)
         )))

(defn solve-formula [rules formula]
  (let [entity (-> formula :v last :v)
        entity-val (-> entity resolve-entity :v)
        rule-selector (->> formula :v (map (fn[fe] [(:gterm fe) (-> fe :v resolve-entity :t)])) flatten vec)
        rule (get-rule rules rule-selector)]
    (cond (and (= :not (first rule)) (or (true? entity-val)(false? entity-val)))
          [(merge-entity entity {:v (not entity-val)})]
          (and (= :split-to (first rule)) (coll? entity-val))
          (->> rule second (map #(merge-entity entity {:t %2 :v %1 :p (get-prob entity rule)}) entity-val) vec)
          (= :split-to (first rule))
          (->> rule second (map #(merge-entity entity {:t %})) vec)
          (= :switch-to (first rule))
          (let [as (if (<= (count rule) 2) :as-is (nth rule 2))
                entity-val (cond (= as :as-first) (if (coll? entity-val) (first entity-val) entity-val)
                                 (= as :as-second) (if (and (coll? entity-val)(second entity-val)) (second entity-val) entity-val)
                                 (= as :as-third) (if (and (coll? entity-val)(> (count entity-val) 2)) (nth 2 entity-val) entity-val)
                                 (= as :as-last) (if (coll? entity-val) (last entity-val) entity-val)
                                 (= as :as-least) (if (seq entity-val) (apply min entity-val) entity-val)
                                 (= as :as-most) (if (seq entity-val) (apply max entity-val) entity-val)
                                 (= as :as-is)  entity-val
                                 (= as :as-not) (if (or (true? entity-val) (false? entity-val)) (not entity-val) entity-val))]
            (->> rule second (#(merge-entity entity {:t % :v entity-val :p (get-prob entity rule)})) vector))
          (and (coll? rule) (seq rule) (nil? (first rule)))
          nil
          :else [entity]
          )))

(defmulti do-clean (fn [strategy coll k] strategy))

(defmethod do-clean :full-reliability
  [strategy coll k] (->> coll (filter #(>= (nth % 3) 0.99)) first))

(defmethod do-clean :boolean-and
  [strategy coll k] (reduce (fn[a b] [k (and (second a)(second b))]) [k true] coll))

(defmethod do-clean :max-likelihood
  [strategy coll k] (->> coll (sort-by #(nth % 3) >) first))

(defmethod do-clean :to-collection
  [strategy coll k] [k (->> coll
                            (map (fn [x] [(first x) (second x)]))
                            tools/reduce-to-map
                            k
                            (#(if (coll? %) (vec %) [%])))])

(defn clean-result [{:keys [default low-reliability-bound] :as rules} extracted]
  (let [rules
        (->>
         (select-keys rules (->> rules keys (remove #{:default :low-reliability-bound})))
         (mapcat (fn [[k v]] (map #(vector % k) v)))
         (into {}))]

        (->> extracted
          (group-by first)
         (map (fn [[k v]] [(if-let [strategy (k rules)] strategy default) v k]))
         (map #(apply do-clean %))
         (map (fn [x] [(first x)(second x)]))
         (filter first)
         (into {})
             )
    ))

(defn adjust-weight [k entity]
  (let [{:keys [t p]} (resolve-entity entity)]
    (if (= t k)
      entity
      (merge-entity entity {:p (* p 0.9)})
      )))

(defn extract-info [{:keys [grammar output-rules] :as model} prepared]
  (->> prepared
       (terminate grammar)
       (filter #(= :formula (:gterm %)))
       (mapcat #(->> %
                     flatten-formula
                     simplify-formula
                     split-formula
                     ))
       (mapcat #(solve-formula output-rules %))
       vec
       ))

(defn extract-from-text [model text]
  (->> text
       (clean-text)
       (clasterize-text (:terms model))
       (parse-text)
       (extract-info model)
       ))

(defn extract-from-map [model m]
  (->> m
       (map (fn [[k v]] [k (map #(adjust-weight k %) (extract-from-text model v))]))
       (mapcat (fn [[k v]] v))
       ))

(defn process-item [model input]
  (let [{:keys [as-is include-origin exclude]
         :or {as-is #{} include-origin #{} exclude #{}}} (:input-rules model)
        input (select-keys input (->> input (map key)(remove exclude)))
        non-string (->> input
                        (remove (fn [[k v]] (string? v)))
                        (map key))

        as-is (set (concat as-is non-string))
        extract (->> input
                     (map key)
                     (remove as-is))
        as-is-map (->> (set (concat as-is include-origin))
                       (select-keys input)
                       (map (fn [[k v]] [k v nil 1])))
        extract-map (select-keys input extract)
        extracted (->> extract
                       (select-keys input)
                       (extract-from-map model)
                       )
        ]
    (->> extracted
         (concat as-is-map)
         (clean-result (:clean-rules model))
         )))

;; SYS need to deep refactor with core ns same func

(def sys (atom {}))

(defn get-config[]
  (conf/cget (:conf @sys)))

(defn init-config [conf]
  (assoc conf :terms
    (->> (:terms conf)
         (map #(assoc % :formatters (form/read-formatter (:formatters %))))
         vec)))

(defn get-lock [task-id]
  (or (get-in @sys [:locks task-id])
      (get-in (swap! sys
                     (fn [x] (if (get-in x [:locks task-id])
                               x
                               (assoc-in x [:locks task-id] (Object.)))))
              [:locks task-id])))

(defn correct-person-name [item]
  (assoc item :person-name ((:dict-names @sys) (:person-name item))))

(defn extract-handler [t {:keys [task-id storage-entity] :as opts}]
  (locking (get-lock task-id)
    (try
    (let [conf (get-config)
          extracted (->>
                      (select storage-entity (where {:extracted 0}))
                      (map (fn [x] [(:id x) (->> (:raw-edn x)
                                                 read-string
                                                 (process-item conf)
                                                 correct-person-name)])))
          ]

     (doseq [[id edn] extracted]
        (update storage-entity
                (set-fields {:extracted 1
                             :extracted-edn (pr-str edn)})
                (where {:id id})))
      (timbre/info "Mariposa extract-handler procceed count: " (count extracted)))
      (catch Exception e (timbre/error e "Error during extraction: " (.getMessage e))))
  ))

;; (time
;;  (extract-handler nil {:task-id :sdf :storage-entity :ads-bu})
;; )

(->> (select :ads-bu (fields :id))
     (take 100)
     (map #(update :ads-bu (set-fields {:extracted 0})(where {:id (:id %)}))))

(defn create-task [task-id conf]
  (let [{:keys [sched opts]:as task-conf} (get-in conf [:tasks task-id])]
    {:id task-id
     :handler extract-handler
     :schedule sched
     :opts (assoc opts :task-id task-id)}))

(defn init []
  (if (:started @sys)
    (timbre/info "Mariposa already started. Use restart or stop.")
    (do
      (timbre/info "Mariposa initialization...")
      (store/initialize (env :database))
      (let [conf-state (conf/file-config (env :mariposa-config))
            conf (conf/cswap! conf-state init-config)
            tasks (map (fn [[k v]] (create-task k conf))(:tasks conf))]
        (reset! sys {:conf conf-state
                     :cronj (sched/cronj :entries tasks)
                     :dict-names (conf/cget(conf/file-config (env :mariposa-dict-names)))}))
      )))

(defn start []
  (if (:started @sys)
    (timbre/info "Mariposa already started.")
    (do
      (timbre/info "Mariposa starting...")
      (if-let [cronj (:cronj @sys)]
        (sched/start! cronj)
        (do (init)(start)))
      (swap! sys assoc :started true)
      (timbre/info "Mariposa started."))))

(defn stop []
  (if (:started @sys)
    (do
      (when-let [cronj (:cronj @sys)] (sched/stop! cronj))
      (timbre/info "Mariposa stopped."))
    (timbre/info "Mariposa already stopped."))
  (swap! sys dissoc :started))

(defn restart []
  (stop)
  (conf/creset!(:conf @sys))
  (start))

;(init)
;(start)
;(stop)
;(restart)



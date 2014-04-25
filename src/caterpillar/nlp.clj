(ns caterpillar.nlp
  (:require
   [caterpillar.tools :as tools]
   [clojure.string :as s]
   ))

;;; levenshtein
(defn damerau-levenshtein-distance
  [a b]
  (let [m (count a)
        n (count b)
        init (apply tools/deep-merge-with (fn [a b] b)
                    (concat
                     ;;deletion
                     (for [i (range 0 (inc m))]
                       {i {0 i}})
                     ;;insertion
                     (for [j (range 0 (inc n))]
                       {0 {j j}})))
        table (reduce
               (fn [d [i j]]
                 (tools/deep-merge-with
                  (fn [a b] b)
                  d
                  (let [cost (tools/bool-to-binary (not (= (nth a (dec i))
                                                     (nth b (dec j)))))
                        x
                        (min
                         (inc ((d (dec i)) j)) ;;deletion
                         (inc ((d i) (dec j))) ;;insertion
                         (+ ((d (dec i))
                             (dec j)) cost)) ;;substitution

                        val (if (and (> i 1)
                                     (> j 1)
                                     (= (nth a (dec i))
                                        (nth b (- j 2)))
                                     (= (nth a (- i 2))
                                        (nth b (dec j))))
                              (min x (+ ((d (- i 2))
                                         (- j 2)) ;;transposition
                                        cost))
                              x)]
                    {i {j val}})))
               init
               (for [j (range 1 (inc n))
                     i (range 1 (inc m))] [i j]))]

    ((table m) n)))

(defn levenshtein-match? [s dict & [dist]]
  (let [dist (or dist (if (> 4 (count s)) 1 2))]
    (some #(>= dist (damerau-levenshtein-distance s %)) dict)))

(defn in-semantics? [text semantics]
  (->> text
       (re-seq #"\p{IsCyrillic}+|\w+")
       (map #(-> % s/trim s/lower-case))
       (some #(levenshtein-match? % semantics))))

(ns caterpillar.config
  (:require
   [clojure.java.io :as io]
   [clojure.tools.reader :as reader]
   [environ.core :refer [env]]
   )
  )

(defn from-clj
  [fname]
  (with-open [rdr (-> (io/resource fname)
                      io/reader
                      java.io.PushbackReader.)]
    (reader/read rdr)))

(defn to-clj
  [fname data]
  (with-open [w (-> (io/resource fname)
                    io/writer)]
    (.write w (pr-str data))))

(defprotocol IConfigState
  (cget [_])
  (cswap! [_ f & args])
  (creset! [_ v][_])
  (csave!! [_]))

(defn file-config [path]
  (let [state (atom nil)]
    (reify
      IConfigState
      (cget [this] (if @state @state (creset! this)))
      (cswap! [_ f & args] (swap! state #(apply f % args)))
      (creset! [this] (creset! this (from-clj path)))
      (creset! [_ v] (reset! state v))
      (csave!! [_] (to-clj path @state)))))

(ns caterpillar.errors
  (:require
   [caterpillar.storage :as store]
   [caterpillar.tools :as tools]
   [cronj.core :as sched]
   [cronj.data.scheduler :as ts]
   [taoensso.timbre :as timbre]
   ))


(defn log-error [{:keys [e message subsystem] :as error-descriptor}]
  (timbre/error error-descriptor)
  (try
    (store/insert-only {:message message
                        :subsystem subsystem
                        :context
                        (-> error-descriptor
                            (dissoc message subsystem e)
                            (assoc :stacktrace (timbre/stacktrace e))
                            pr-str)} :errors)
    (catch Exception e (timbre/error e))))

(defmacro with-try [context & body]
  `(try
     ~@body
     (catch Exception e# (log-error (merge (or ~context {})
                                           {:subsystem (str *ns*) :e e#
                                            :message (.getMessage e#)
                                            :exec-body (pr-str '~@body)})))))

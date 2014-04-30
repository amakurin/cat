(ns caterpillar.system
  (:require
   [clj-time.core :as tc]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [environ.core :refer [env]]
   )
 )
;; ok in order code not to look realy trashy
;; i've decided implement this temporary abstraction for subsystem management
;; we take configs from project settings
;; everyone of which may or may not contain tasks section
;; if task descriptors are found we use them with scheduler
;; we also use namespace of subsystem to find fns with meta
;; - task-handler Required if task descriptors are specified
;; - system-initializer Optional

;;         :systems {:caterpillar
;;                   {:config "conf/crawl.clj"
;;                    :namespace caterpillar.core}
;;                   :mariposa
;;                   {:config "conf/extract.clj"
;;                    :namespace caterpillar.mariposa}
;;                   }


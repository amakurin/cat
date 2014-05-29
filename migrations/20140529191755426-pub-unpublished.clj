;; migrations/20140529191755426-pub-unpublished.clj

(defn up []
  ["ALTER TABLE `pub` ADD `unpub` tinyint(1) NOT NULL DEFAULT 0;"])

(defn down []
  [])

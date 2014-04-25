;; migrations/20140423035722885-initial-scheme.clj

(defn up []
  [
   "CREATE TABLE IF NOT EXISTS `seen` (
   `src-id` varchar(40) NOT NULL COMMENT 'id on source'
   ,`target` varchar(40) NOT NULL COMMENT 'crawler target'
   ,KEY (`src-id`,`target`)
   ) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT 'seen link ids'"

   "CREATE TABLE `agents` (
   `phone` varchar(10) NOT NULL COMMENT 'agent phone'
   ,`changed` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
   ,`city` varchar(30) NOT NULL COMMENT 'agent city'
   ,`target` varchar(40) NOT NULL COMMENT 'crawler target'
   ,`url` varchar(1000) NOT NULL COMMENT 'evidence url'
   ,`updates` BIGINT NOT NULL DEFAULT 1 COMMENT 'Total count of updates (times this agent was seen)'
   ,PRIMARY KEY (`phone`)
   ,KEY (`phone`,`city`)
   ) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='Agents phones collector'"

   "CREATE TRIGGER `inc_updates` BEFORE UPDATE ON `agents`
   FOR EACH ROW SET NEW.updates = NEW.updates + 1"

   "CREATE  TABLE `ads` (
   `id` BIGINT NOT NULL AUTO_INCREMENT
   ,`src-id` VARCHAR(40) NOT NULL
   ,`target` VARCHAR(40) NOT NULL
   ,`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'creation date of record'
   ,`city` varchar(30) NOT NULL COMMENT 'agent city'
   ,`raw-edn` TEXT NULL COMMENT 'edn map with raw (text, unprocessed) data aka facts'
   ,`url` VARCHAR(1000) NOT NULL COMMENT 'url of ad'
   ,PRIMARY KEY (`id`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = 'ads from potential owners'"
   ])

(defn down []
  ["drop seen"])

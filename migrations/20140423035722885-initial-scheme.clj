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
   ,`updates` BIGINT NOT NULL DEFAULT 1 COMMENT 'total count of updates (times this agent was seen)'
   ,PRIMARY KEY (`phone`)
   ,KEY (`phone`,`city`)
   ) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='agents phones collector'"

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
   ,`extracted` tinyint(1) NOT NULL DEFAULT 0
   ,`extracted-edn` text COMMENT 'extracted facts in edn (null if not yet extracted)'
   ,`classified` tinyint(1) NOT NULL DEFAULT 0
   ,`verdict` varchar(40) DEFAULT NULL COMMENT 'classification result (initial null means not classified yet)'
   ,`history-edn` text COMMENT 'history of verdicts in edn format (includes value, reason, and date of verdict. initial null for absent verdict case)'
   ,PRIMARY KEY (`id`)
   ,KEY (`extracted`)
   ,KEY (`classified`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = 'ads from potential owners'"

   "CREATE  TABLE `app-type-groups` (
   `id` SMALLINT NOT NULL
   ,`mnemo` VARCHAR(40) NOT NULL
   ,`name` VARCHAR(100) NOT NULL
   ,PRIMARY KEY (`id`)
   ,KEY (`mnemo`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = ''"

   "insert into `app-type-groups`
   (id, mnemo, name)
   values
   (0, ':studio', 'студия')
   ,(1, ':appartment', 'квартира')
   ,(2, ':room', 'комната')
   "

   "CREATE  TABLE `appartment-types` (
   `id` SMALLINT NOT NULL
   ,`mnemo` VARCHAR(40) NOT NULL
   ,`name` VARCHAR(100) NOT NULL
   ,`group` SMALLINT NOT NULL
   ,`rooms` SMALLINT NOT NULL
   ,PRIMARY KEY (`id`)
   ,KEY (`mnemo`)
   ,KEY (`group`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = 'appartment types'"

   "insert into `appartment-types`
   (id, mnemo, name, `group`, rooms)
   values
   (0, ':studio', 'студия', 0, 1)
   ,(1, ':appartment1', '1-комн.кв.', 1, 1)
   ,(2, ':appartment2', '2-комн.кв.', 1, 2)
   ,(3, ':appartment3', '3-комн.кв.', 1, 3)
   ,(4, ':appartment4', '4-комн.кв.', 1, 4)
   ,(5, ':appartment5', '5-комн.кв.', 1, 5)
   ,(6, ':appartment6', '6-комн.кв.', 1, 6)
   ,(7, ':appartment7', '7-комн.кв.', 1, 7)
   ,(8, ':room', 'комната', 2, 1)
   "

   "CREATE  TABLE `building-types` (
   `id` SMALLINT NOT NULL
   ,`mnemo` VARCHAR(40) NOT NULL
   ,`name` VARCHAR(100) NOT NULL
   ,PRIMARY KEY (`id`)
   ,KEY (`mnemo`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = ''"

   "insert into `building-types`
   (id, mnemo, name)
   values
   (0, ':brick', 'кирпичный')
   ,(1, ':panel', 'панельный')
   ,(2, ':block', 'блочный')
   ,(3, ':monolit', 'монолитный')
   ,(4, ':wood', 'деревянный')
   "
   "CREATE  TABLE `layout-types` (
   `id` SMALLINT NOT NULL
   ,`mnemo` VARCHAR(40) NOT NULL
   ,`name` VARCHAR(100) NOT NULL
   ,PRIMARY KEY (`id`)
   ,KEY (`mnemo`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = ''"

   "insert into `layout-types`
   (id, mnemo, name)
   values
   (0, ':studio', 'раздельный')
   ,(1, ':appartment', 'совмещенный')
   "
   ])

(defn down []
  ["drop seen"])

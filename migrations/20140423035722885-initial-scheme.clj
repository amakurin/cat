;; migrations/20140423035722885-initial-scheme.clj

(defn up []
  [
   "CREATE TABLE IF NOT EXISTS `errors` (
   `id` bigint(20) NOT NULL AUTO_INCREMENT
   ,`created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
   ,`subsystem` varchar(100) DEFAULT NULL
   ,`message` varchar(1000) DEFAULT NULL
   ,`context` longtext NULL
   ,PRIMARY KEY (`id`)
   ) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='errors collector'"

   "CREATE TABLE IF NOT EXISTS `seen` (
   `src-id` varchar(40) NOT NULL COMMENT 'id on source'
   ,`target` varchar(40) NOT NULL COMMENT 'crawler target'
   ,KEY (`src-id`,`target`)
   ) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT 'seen link ids'"

   "CREATE TABLE IF NOT EXISTS `agents` (
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

   "CREATE TABLE IF NOT EXISTS `ads` (
   `id` BIGINT NOT NULL AUTO_INCREMENT
   ,`src-id` VARCHAR(40) NOT NULL
   ,`target` VARCHAR(40) NOT NULL
   ,`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'creation date of record'
   ,`city` varchar(30) NOT NULL COMMENT 'agent city'
   ,`raw-edn` TEXT NULL COMMENT 'edn map with raw (text, unprocessed) data aka facts'
   ,`url` VARCHAR(1000) NOT NULL COMMENT 'url of ad'
   ,`extracted` tinyint(1) NOT NULL DEFAULT 0
   ,`extracted-edn` text COMMENT 'extracted facts in edn (null if not yet extracted)'
   ,`verdict` smallint(6) NOT NULL DEFAULT '-1'
   ,`history-edn` text COMMENT 'history of verdicts in edn format (includes value, reason, and date of verdict. initial null for absent verdict case)'
   ,`published` tinyint(1) NOT NULL DEFAULT 0
   ,PRIMARY KEY (`id`)
   ,KEY (`extracted`)
   ,KEY (`verdict`)
   ,KEY (`published`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = 'ads from potential owners'"

   "CREATE TABLE IF NOT EXISTS `app-type-groups` (
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

   "CREATE TABLE IF NOT EXISTS `appartment-types` (
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

   "CREATE TABLE IF NOT EXISTS `building-types` (
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

   "CREATE TABLE IF NOT EXISTS `layout-types` (
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

   "CREATE TABLE IF NOT EXISTS `cities` (
   `id` SMALLINT NOT NULL
   ,`mnemo` VARCHAR(40) NOT NULL
   ,`name` VARCHAR(100) NOT NULL
   ,`use-districts` tinyint(1) NOT NULL DEFAULT 1
   ,`use-metro` tinyint(1) NOT NULL DEFAULT 1
   ,`osm-file-name` VARCHAR(100) NOT NULL
   ,`metro-map-file-name` VARCHAR(100) NULL
   ,PRIMARY KEY (`id`)
   ,KEY (`mnemo`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = ''"

   "insert into `cities`
   (id, mnemo, name,`osm-file-name` )
   values
   (0, ':smr', 'Самара','samara.xml' )
   "

   "CREATE TABLE IF NOT EXISTS `districts` (
   `id` INT NOT NULL AUTO_INCREMENT
   ,`city-id` SMALLINT NOT NULL
   ,`mnemo` VARCHAR(40) NULL
   ,`name` VARCHAR(100) NOT NULL
   ,`osm-relation-id` VARCHAR(40) NOT NULL
   ,PRIMARY KEY (`id`)
   ,KEY (`mnemo`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = ''"

   "insert into `districts`
   (`city-id`, name, `osm-relation-id`)
   values
   (0, 'Куйбышевский', '283540')
   ,(0, 'Самарский', '283541')
   ,(0, 'Железнодорожный', '283645')
   ,(0, 'Ленинский', '283781')
   ,(0, 'Октябрьский', '284542')
   ,(0, 'Советский', '284582')
   ,(0, 'Промышленный', '285136')
   ,(0, 'Кировский', '285953')
   ,(0, 'Красноглинский', '285954')
   "

   "CREATE TABLE IF NOT EXISTS `metros` (
   `id` INT NOT NULL AUTO_INCREMENT
   ,`city-id` SMALLINT NOT NULL
   ,`mnemo` VARCHAR(40) NULL
   ,`name` VARCHAR(100) NOT NULL
   ,`lat` varchar(60) NOT NULL
   ,`lng` varchar(60) NOT NULL
   ,`map-x` int DEFAULT NULL
   ,`map-y` int DEFAULT NULL
   ,PRIMARY KEY (`id`)
   ,KEY (`mnemo`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = ''"

   "insert into `metros`
   (`city-id`, name, lat, lng)
   values
   (0,  'Алабинская', '53.210082', '50.135951')
   ,(0, 'Российская', '53.212015', '50.148993')
   ,(0, 'Московская', '53.202841', '50.160463')
   ,(0, 'Гагаринская','53.200240', '50.176056')
   ,(0, 'Спортивная', '53.200910', '50.200180')
   ,(0, 'Советская',  '53.201563', '50.221947')
   ,(0, 'Победа', 	 '53.206674', '50.235144' )
   ,(0, 'Безымянка',  '53.212822', '50.248574')
   ,(0, 'Кировская',  '53.211227', '50.269460')
   ,(0, 'Юнгородок',  '53.212753', '50.283315')"

   "CREATE TABLE IF NOT EXISTS `verdicts` (
   `id` INT NOT NULL
   ,`mnemo` VARCHAR(40) NULL
   ,`name` VARCHAR(100) NOT NULL
   ,PRIMARY KEY (`id`)
   ,KEY (`mnemo`)
   )ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT = ''"

   "insert into `verdicts`
   (`id`, name)
   values
   (0,  'Собственник')
   ,(5, 'Подозреваемый')
   ,(6, 'Нет номера')
   ,(9, 'Абсурдный номер')
   ,(10, 'Агент')
   "

   "CREATE TABLE IF NOT EXISTS `pub` (
   `id` bigint(20) NOT NULL
   ,`seoid` varchar(200) COLLATE utf8_bin NOT NULL
   ,`created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'creation date of record'
   ,`src-date` datetime DEFAULT NULL
   ,`city` smallint(6) DEFAULT NULL
   ,`appartment-type` smallint(6) DEFAULT NULL
   ,`floor` smallint(6) DEFAULT NULL
   ,`floors` smallint(6) DEFAULT NULL
   ,`price` decimal(10,0) DEFAULT NULL
   ,`person-name` varchar(200) DEFAULT NULL
   ,`phone` varchar(200) NOT NULL
   ,`address` varchar(300) DEFAULT NULL
   ,`district` int DEFAULT NULL
   ,`metro` int DEFAULT NULL
   ,`distance` int DEFAULT NULL
   ,`lat` varchar(60) DEFAULT NULL
   ,`lng` varchar(60) DEFAULT NULL
   ,`total-area` decimal(5,2) DEFAULT NULL
   ,`living-area` decimal(5,2) DEFAULT NULL
   ,`kitchen-area` decimal(5,2) DEFAULT NULL
   ,`toilet` smallint(6) DEFAULT NULL
   ,`building-type` smallint(6) DEFAULT NULL
   ,`description` varchar(2000) DEFAULT NULL
   ,`imgs` varchar(2000) DEFAULT NULL

   ,`deposit` tinyint(1) DEFAULT NULL
   ,`counters` tinyint(1) DEFAULT NULL
   ,`plus-utilities` tinyint(1) DEFAULT NULL
   ,`plus-electricity` tinyint(1) DEFAULT NULL
   ,`plus-water` tinyint(1) DEFAULT NULL
   ,`plus-gas` tinyint(1) DEFAULT NULL

   ,`balcony` tinyint(1) DEFAULT NULL
   ,`loggia` tinyint(1) DEFAULT NULL
   ,`bow-window` tinyint(1) DEFAULT NULL

   ,`furniture` tinyint(1) DEFAULT NULL
   ,`internet` tinyint(1) DEFAULT NULL
   ,`tv` tinyint(1) DEFAULT NULL
   ,`frige` tinyint(1) DEFAULT NULL
   ,`washer` tinyint(1) DEFAULT NULL
   ,`conditioner` tinyint(1) DEFAULT NULL

   ,`parking` tinyint(1) DEFAULT NULL

   ,`intercom` tinyint(1) DEFAULT NULL
   ,`security` tinyint(1) DEFAULT NULL
   ,`concierge` tinyint(1) DEFAULT NULL

   ,`only-russo` tinyint(1) DEFAULT NULL
   ,`kids` tinyint(1) DEFAULT NULL
   ,`pets` tinyint(1) DEFAULT NULL
   ,`addiction` tinyint(1) DEFAULT NULL
   ,PRIMARY KEY (`id`)
   ,UNIQUE KEY (`seoid`)
   ,KEY (`src-date`)
   ,KEY (`city`, `appartment-type`)
   ,KEY (`city`, `appartment-type`, `price`)
   )ENGINE= MyISAM DEFAULT CHARSET=utf8 COMMENT='published'"
   ])

(defn down []
  ["drop seen"])

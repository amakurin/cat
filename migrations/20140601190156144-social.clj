;; migrations/20140601190156144-social.clj

(defn up []
  ["CREATE TABLE IF NOT EXISTS `social` (
   `id` SMALLINT NOT NULL
   ,`mnemo` VARCHAR(40) NOT NULL
   ,`owner-id` VARCHAR(40) NOT NULL
   ,`owner-url` VARCHAR(400) NOT NULL
   ,`auth-url` VARCHAR(400) NOT NULL
   ,`api-url` VARCHAR(400) NOT NULL
   ,`api-ver` VARCHAR(4) NULL
   ,`client-id` VARCHAR(200) NOT NULL
   ,`client-secret` VARCHAR(1000) NULL
   ,`auth-token` VARCHAR(1000) NULL
   ,`subsystem-key` varchar(100) DEFAULT NULL
   ,`pub-period-sec` SMALLINT DEFAULT 360
   ,PRIMARY KEY (`id`)
   ,KEY (`mnemo`)
   ) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='social configuration'"

   "ALTER TABLE `pub` ADD `soc-vk` SMALLINT DEFAULT NULL;"])

(defn down []
  [])

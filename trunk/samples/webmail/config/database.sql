SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

CREATE TABLE IF NOT EXISTS `Post` (
  `id` bigint(20) NOT NULL auto_increment,
  `authorUserId` bigint(20) NOT NULL,
  `targetUserId` bigint(20) NOT NULL,
  `text` text NOT NULL,
  `creationTime` datetime NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `authorUserId` (`authorUserId`),
  KEY `targetUserId` (`targetUserId`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;

CREATE TABLE IF NOT EXISTS `User` (
  `id` bigint(20) NOT NULL auto_increment,
  `login` varchar(255) NOT NULL,
  `password` varchar(255) default NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `login` (`login`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;

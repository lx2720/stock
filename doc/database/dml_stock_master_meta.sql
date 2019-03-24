INSERT INTO `task` (`id`, `name`, `state`, `description`) VALUES ('1', 'begin_of_year', '1', 'begin of year');
INSERT INTO `task` (`id`, `name`, `state`, `description`) VALUES ('2', 'end_of_year', '1', 'end of year');
INSERT INTO `task` (`id`, `name`, `state`, `description`) VALUES ('3', 'begin_of_day', '1', 'begin of day');
INSERT INTO `task` (`id`, `name`, `state`, `description`) VALUES ('4', 'end_of_day', '1', 'end of day');
INSERT INTO `task` (`id`, `name`, `state`, `description`) VALUES ('5', 'update_of_stock', '1', 'update of stock');
INSERT INTO `task` (`id`, `name`, `state`, `description`) VALUES ('6', 'update_of_stock_state', '1', 'update of stock state');
INSERT INTO `task` (`id`, `name`, `state`, `description`) VALUES ('7', 'update_of_daily_index', '1', 'update of daily index');
INSERT INTO `task` (`id`, `name`, `state`, `description`) VALUES ('8', 'ticker', '1', 'ticker');

INSERT INTO `execute_info` (`id`, `task_id`, `state`) VALUES ('1', '1', '2');
INSERT INTO `execute_info` (`id`, `task_id`, `state`) VALUES ('2', '2', '0');
INSERT INTO `execute_info` (`id`, `task_id`, `state`) VALUES ('3', '3', '2');
INSERT INTO `execute_info` (`id`, `task_id`, `state`) VALUES ('4', '4', '0');
INSERT INTO `execute_info` (`id`, `task_id`, `state`) VALUES ('5', '5', '2');
INSERT INTO `execute_info` (`id`, `task_id`, `state`) VALUES ('6', '6', '0');
INSERT INTO `execute_info` (`id`, `task_id`, `state`) VALUES ('7', '7', '2');
INSERT INTO `execute_info` (`id`, `task_id`, `state`) VALUES ('8', '8', '2');

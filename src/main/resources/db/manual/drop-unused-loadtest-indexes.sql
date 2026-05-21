-- Drop write-heavy indexes that are not needed for the current k6 checkout flow.
-- Run this once on the target MySQL database after removing the matching @Index declarations.

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'orders'
       AND index_name = 'idx_orders_status_created_at') > 0,
    'ALTER TABLE orders DROP INDEX idx_orders_status_created_at',
    'SELECT ''idx_orders_status_created_at not found''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'inventory_logs'
       AND index_name = 'idx_inventory_logs_inventory_created_at') > 0,
    'ALTER TABLE inventory_logs DROP INDEX idx_inventory_logs_inventory_created_at',
    'SELECT ''idx_inventory_logs_inventory_created_at not found''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'notifications'
       AND index_name = 'idx_notifications_user_read_created_at') > 0,
    'ALTER TABLE notifications DROP INDEX idx_notifications_user_read_created_at',
    'SELECT ''idx_notifications_user_read_created_at not found''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

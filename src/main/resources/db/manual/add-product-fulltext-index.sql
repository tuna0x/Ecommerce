-- Add the FULLTEXT index used by ProductRepository.searchByFullText.
-- Hibernate ddl-auto=update does not manage MySQL FULLTEXT indexes, so run this once per database.

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'products'
       AND index_name = 'ft_products_name_name_unsigned') = 0,
    'ALTER TABLE products ADD FULLTEXT INDEX ft_products_name_name_unsigned (name, name_unsigned)',
    'SELECT ''ft_products_name_name_unsigned already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

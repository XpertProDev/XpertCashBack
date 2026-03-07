-- Retirer les colonnes de formatage si elles ont été ajoutées (date_creation_formatee, designation_avec_date_fr)
-- Idempotent : ne fait rien si les colonnes n'existent pas

SET @col1_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entree_generale' AND COLUMN_NAME = 'date_creation_formatee');
SET @sql1 = IF(@col1_exists > 0, 'ALTER TABLE entree_generale DROP COLUMN date_creation_formatee', 'SELECT 1');
PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @col2_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entree_generale' AND COLUMN_NAME = 'designation_avec_date_fr');
SET @sql2 = IF(@col2_exists > 0, 'ALTER TABLE entree_generale DROP COLUMN designation_avec_date_fr', 'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Rollback des migrations V1_22 à V1_27 (à exécuter dans l'ordre inverse des appliquées).
-- Attention : supprime données et schéma du module assistance et annule dette_type_origine / colonnes formatage.

-- ========== Annuler V1_27 : recréer les colonnes supprimées sur entree_generale ==========
SET @col1_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entree_generale' AND COLUMN_NAME = 'date_creation_formatee');
SET @sql1 = IF(@col1_exists = 0,
    'ALTER TABLE entree_generale ADD COLUMN date_creation_formatee VARCHAR(100) NULL',
    'SELECT 1');
PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @col2_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entree_generale' AND COLUMN_NAME = 'designation_avec_date_fr');
SET @sql2 = IF(@col2_exists = 0,
    'ALTER TABLE entree_generale ADD COLUMN designation_avec_date_fr VARCHAR(500) NULL',
    'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- ========== Annuler V1_26 : remettre dette_type_origine à NULL (données remplies par le backfill) ==========
UPDATE entree_generale SET dette_type_origine = NULL WHERE dette_type_origine IS NOT NULL;

-- ========== Annuler V1_25 : supprimer la colonne dette_type_origine ==========
SET @col_dto = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entree_generale' AND COLUMN_NAME = 'dette_type_origine');
SET @sql_dto = IF(@col_dto > 0, 'ALTER TABLE entree_generale DROP COLUMN dette_type_origine', 'SELECT 1');
PREPARE stmt_dto FROM @sql_dto;
EXECUTE stmt_dto;
DEALLOCATE PREPARE stmt_dto;

-- ========== Annuler V1_24 : supprimer valide_par_client sur assistance_ticket ==========
SET @col_vpc = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'assistance_ticket' AND COLUMN_NAME = 'valide_par_client');
SET @sql_vpc = IF(@col_vpc > 0, 'ALTER TABLE assistance_ticket DROP COLUMN valide_par_client', 'SELECT 1');
PREPARE stmt_vpc FROM @sql_vpc;
EXECUTE stmt_vpc;
DEALLOCATE PREPARE stmt_vpc;

-- ========== Annuler V1_22 : supprimer les tables du module assistance (avant suppression users SUPPORT) ==========
DROP TABLE IF EXISTS assistance_message;
DROP TABLE IF EXISTS assistance_ticket;

-- ========== Annuler V1_23 : remettre role.name en ENUM (sans SUPPORT) ==========
-- Supprimer les utilisateurs et le rôle SUPPORT pour éviter erreur de conversion.
DELETE FROM user WHERE role_id IN (SELECT id FROM role WHERE name = 'SUPPORT');
DELETE FROM role WHERE name = 'SUPPORT';

SET @role_name_type = (SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role' AND COLUMN_NAME = 'name');
SET @sql_role = IF(@role_name_type IS NOT NULL AND @role_name_type NOT LIKE '%enum%',
    'ALTER TABLE role MODIFY COLUMN name ENUM(''ADMIN'',''MANAGER'',''VENDEUR'') NOT NULL',
    'SELECT 1');
PREPARE stmt_role FROM @sql_role;
EXECUTE stmt_role;
DEALLOCATE PREPARE stmt_role;

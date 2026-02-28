-- Quota d'utilisateurs par entreprise (admin inclus). Défaut 2 = admin + 1 utilisateur.
-- Entreprises existantes : max = 2. On garde les 2 premiers inscrits, on bloque les autres.
-- Migration idempotente : peut être exécutée plusieurs fois sans erreur.

-- 1. Ajouter max_utilisateurs à entreprise si la colonne n'existe pas
SET @col_ent = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entreprise' AND COLUMN_NAME = 'max_utilisateurs');
SET @sql_ent = IF(@col_ent = 0, 'ALTER TABLE entreprise ADD COLUMN max_utilisateurs INT NULL', 'SELECT 1');
PREPARE stmt_ent FROM @sql_ent;
EXECUTE stmt_ent;
DEALLOCATE PREPARE stmt_ent;

UPDATE entreprise SET max_utilisateurs = 2 WHERE max_utilisateurs IS NULL;

-- 2. Ajouter locked_by_quota à user si la colonne n'existe pas
SET @col_user = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'locked_by_quota');
SET @sql_user = IF(@col_user = 0, 'ALTER TABLE `user` ADD COLUMN locked_by_quota TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt_user FROM @sql_user;
EXECUTE stmt_user;
DEALLOCATE PREPARE stmt_user;

-- 3. Bloquer les utilisateurs au-delà des 2 premiers par entreprise (ordre : created_at, id).
-- Les 2 premiers (Admin + 2e inscrit) restent actifs ; les autres sont locked + locked_by_quota.
-- Note : ROW_NUMBER() nécessite MySQL 8+. Table `user` entre backticks (mot réservé).
UPDATE `user` u
INNER JOIN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY entreprise_id ORDER BY created_at, id) AS rn
        FROM `user`
    ) t
    WHERE rn > 2
) excess ON u.id = excess.id
SET u.locked = 1, u.locked_by_quota = 1;

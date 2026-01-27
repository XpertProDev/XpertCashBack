-- Migration corrective pour forcer le nettoyage des ordonnateur_id invalides
-- et supprimer définitivement la colonne enum ordonnateur
-- Migration idempotente : peut être exécutée plusieurs fois sans erreur

-- Étape 1: Supprimer définitivement l'ancienne colonne ordonnateur (enum) si elle existe encore
SET @old_col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND COLUMN_NAME = 'ordonnateur'
);

SET @sql_drop_old = IF(@old_col_exists > 0,
    'ALTER TABLE depense_generale DROP COLUMN ordonnateur',
    'SELECT "Colonne ordonnateur n''existe plus" AS message'
);

PREPARE stmt_drop_old FROM @sql_drop_old;
EXECUTE stmt_drop_old;
DEALLOCATE PREPARE stmt_drop_old;

-- Étape 2: S'assurer que la colonne ordonnateur_id existe
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND COLUMN_NAME = 'ordonnateur_id'
);

SET @sql_add_col = IF(@col_exists = 0,
    'ALTER TABLE depense_generale ADD COLUMN ordonnateur_id BIGINT NULL',
    'SELECT "Colonne ordonnateur_id existe déjà" AS message'
);

PREPARE stmt_add_col FROM @sql_add_col;
EXECUTE stmt_add_col;
DEALLOCATE PREPARE stmt_add_col;

-- Étape 3: Rendre la colonne nullable temporairement pour pouvoir nettoyer
ALTER TABLE depense_generale MODIFY COLUMN ordonnateur_id BIGINT NULL;

-- Étape 4: Supprimer la contrainte de clé étrangère si elle existe (pour pouvoir nettoyer)
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND CONSTRAINT_NAME = 'fk_depense_generale_ordonnateur'
);

SET @sql_drop_fk = IF(@fk_exists > 0,
    'ALTER TABLE depense_generale DROP FOREIGN KEY fk_depense_generale_ordonnateur',
    'SELECT "Contrainte FK n''existe pas" AS message'
);

PREPARE stmt_drop_fk FROM @sql_drop_fk;
EXECUTE stmt_drop_fk;
DEALLOCATE PREPARE stmt_drop_fk;

-- Étape 5: Nettoyer TOUS les ordonnateur_id invalides (0 ou qui n'existent pas dans user)
UPDATE depense_generale dg
LEFT JOIN user u ON dg.ordonnateur_id = u.id
SET dg.ordonnateur_id = NULL
WHERE dg.ordonnateur_id = 0 
   OR (dg.ordonnateur_id IS NOT NULL AND u.id IS NULL);

-- Étape 6: Utiliser cree_par_id comme ordonnateur par défaut pour les NULL
UPDATE depense_generale dg
INNER JOIN user u ON dg.cree_par_id = u.id
SET dg.ordonnateur_id = dg.cree_par_id
WHERE dg.ordonnateur_id IS NULL AND dg.cree_par_id IS NOT NULL;

-- Étape 7: Pour les cas où cree_par_id n'existe pas, utiliser le premier admin/manager de l'entreprise
UPDATE depense_generale dg
SET dg.ordonnateur_id = (
    SELECT u2.id 
    FROM user u2 
    INNER JOIN role r ON u2.role_id = r.id
    WHERE u2.entreprise_id = dg.entreprise_id 
    AND (r.name = 'ADMIN' OR r.name = 'MANAGER')
    ORDER BY u2.id ASC
    LIMIT 1
)
WHERE dg.ordonnateur_id IS NULL;

-- Étape 8: Si toujours NULL, utiliser le premier utilisateur de l'entreprise
UPDATE depense_generale dg
SET dg.ordonnateur_id = (
    SELECT u2.id 
    FROM user u2 
    WHERE u2.entreprise_id = dg.entreprise_id 
    ORDER BY u2.id ASC
    LIMIT 1
)
WHERE dg.ordonnateur_id IS NULL;

-- Étape 9: Vérifier qu'il ne reste plus de NULL
SET @null_count = (
    SELECT COUNT(*) 
    FROM depense_generale 
    WHERE ordonnateur_id IS NULL
);

-- Si des NULL restent, on les laisse (cas extrême où aucune entreprise n'a d'utilisateur)
-- Sinon, rendre la colonne NOT NULL
SET @sql_make_not_null = IF(@null_count = 0,
    'ALTER TABLE depense_generale MODIFY COLUMN ordonnateur_id BIGINT NOT NULL',
    'SELECT "Il reste des NULL, colonne maintenue nullable" AS message'
);

PREPARE stmt_not_null FROM @sql_make_not_null;
EXECUTE stmt_not_null;
DEALLOCATE PREPARE stmt_not_null;

-- Étape 10: Réajouter la contrainte de clé étrangère
SET @fk_exists_after = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND CONSTRAINT_NAME = 'fk_depense_generale_ordonnateur'
);

SET @sql_add_fk = IF(@fk_exists_after = 0,
    'ALTER TABLE depense_generale ADD CONSTRAINT fk_depense_generale_ordonnateur FOREIGN KEY (ordonnateur_id) REFERENCES user(id) ON DELETE RESTRICT',
    'SELECT "Contrainte FK existe déjà" AS message'
);

PREPARE stmt_add_fk FROM @sql_add_fk;
EXECUTE stmt_add_fk;
DEALLOCATE PREPARE stmt_add_fk;

-- Migration pour changer ordonnateur de enum vers user_id
-- Migration idempotente : peut être exécutée plusieurs fois sans erreur

-- Étape 1: Ajouter la nouvelle colonne ordonnateur_id si elle n'existe pas déjà
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND COLUMN_NAME = 'ordonnateur_id'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE depense_generale ADD COLUMN ordonnateur_id BIGINT NULL',
    'SELECT "Colonne ordonnateur_id existe déjà" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Étape 2: Rendre la colonne nullable temporairement pour pouvoir nettoyer les données
ALTER TABLE depense_generale MODIFY COLUMN ordonnateur_id BIGINT NULL;

-- Étape 3: Nettoyer les données invalides - mettre NULL pour les ordonnateur_id qui n'existent pas dans user ou qui sont 0
UPDATE depense_generale dg
LEFT JOIN user u ON dg.ordonnateur_id = u.id
SET dg.ordonnateur_id = NULL
WHERE (dg.ordonnateur_id IS NOT NULL AND u.id IS NULL) 
   OR dg.ordonnateur_id = 0;

-- Étape 4: Migrer les données existantes - utiliser cree_par_id comme ordonnateur par défaut pour les NULL
UPDATE depense_generale dg
INNER JOIN user u ON dg.cree_par_id = u.id
SET dg.ordonnateur_id = dg.cree_par_id
WHERE dg.ordonnateur_id IS NULL AND dg.cree_par_id IS NOT NULL;

-- Étape 5: Pour les cas où cree_par_id n'existe pas non plus, utiliser le premier admin de l'entreprise
UPDATE depense_generale dg
LEFT JOIN user u ON dg.ordonnateur_id = u.id
SET dg.ordonnateur_id = (
    SELECT u2.id 
    FROM user u2 
    INNER JOIN role r ON u2.role_id = r.id
    WHERE u2.entreprise_id = dg.entreprise_id 
    AND (r.name = 'ADMIN' OR r.name = 'MANAGER')
    ORDER BY u2.id ASC
    LIMIT 1
)
WHERE dg.ordonnateur_id IS NULL AND u.id IS NULL;

-- Étape 6: Si toujours NULL, utiliser le premier utilisateur de l'entreprise
UPDATE depense_generale dg
LEFT JOIN user u ON dg.ordonnateur_id = u.id
SET dg.ordonnateur_id = (
    SELECT u2.id 
    FROM user u2 
    WHERE u2.entreprise_id = dg.entreprise_id 
    ORDER BY u2.id ASC
    LIMIT 1
)
WHERE dg.ordonnateur_id IS NULL AND u.id IS NULL;

-- Étape 7: Rendre la colonne NOT NULL (elle est déjà nullable à ce stade)
SET @is_nullable = (
    SELECT IS_NULLABLE 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND COLUMN_NAME = 'ordonnateur_id'
);

-- Rendre la colonne NOT NULL maintenant que toutes les données sont valides
ALTER TABLE depense_generale MODIFY COLUMN ordonnateur_id BIGINT NOT NULL;

-- Étape 8: Ajouter la contrainte de clé étrangère si elle n'existe pas déjà
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND CONSTRAINT_NAME = 'fk_depense_generale_ordonnateur'
);

SET @sql3 = IF(@fk_exists = 0,
    'ALTER TABLE depense_generale ADD CONSTRAINT fk_depense_generale_ordonnateur FOREIGN KEY (ordonnateur_id) REFERENCES user(id) ON DELETE RESTRICT',
    'SELECT "Contrainte fk_depense_generale_ordonnateur existe déjà" AS message'
);

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- Étape 9: Supprimer l'ancienne colonne ordonnateur (enum) si elle existe encore
SET @old_col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND COLUMN_NAME = 'ordonnateur'
);

SET @sql4 = IF(@old_col_exists > 0,
    'ALTER TABLE depense_generale DROP COLUMN ordonnateur',
    'SELECT "Colonne ordonnateur n''existe plus" AS message'
);

PREPARE stmt4 FROM @sql4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

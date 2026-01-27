-- Migration pour nettoyer les ordonnateur_id invalides (0 ou inexistants)
-- Migration idempotente : peut être exécutée plusieurs fois sans erreur

-- Étape 1: Nettoyer les ordonnateur_id = 0 ou qui n'existent pas dans la table user
-- Rendre la colonne nullable temporairement si elle ne l'est pas déjà
SET @is_nullable = (
    SELECT IS_NULLABLE 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND COLUMN_NAME = 'ordonnateur_id'
);

-- Si la colonne est NOT NULL, la rendre nullable temporairement
SET @sql_temp_nullable = IF(@is_nullable = 'NO',
    'ALTER TABLE depense_generale MODIFY COLUMN ordonnateur_id BIGINT NULL',
    'SELECT "Colonne déjà nullable" AS message'
);

PREPARE stmt_temp FROM @sql_temp_nullable;
EXECUTE stmt_temp;
DEALLOCATE PREPARE stmt_temp;

-- Étape 2: Mettre NULL pour les ordonnateur_id invalides (0 ou inexistants)
UPDATE depense_generale dg
LEFT JOIN user u ON dg.ordonnateur_id = u.id
SET dg.ordonnateur_id = NULL
WHERE (dg.ordonnateur_id IS NOT NULL AND u.id IS NULL) 
   OR dg.ordonnateur_id = 0
   OR dg.ordonnateur_id IS NULL;

-- Étape 3: Utiliser cree_par_id comme ordonnateur par défaut pour les NULL
UPDATE depense_generale dg
INNER JOIN user u ON dg.cree_par_id = u.id
SET dg.ordonnateur_id = dg.cree_par_id
WHERE dg.ordonnateur_id IS NULL AND dg.cree_par_id IS NOT NULL;

-- Étape 4: Pour les cas où cree_par_id n'existe pas non plus, utiliser le premier admin de l'entreprise
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

-- Étape 5: Si toujours NULL, utiliser le premier utilisateur de l'entreprise
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

-- Étape 6: Vérifier qu'il ne reste plus de NULL (si oui, on garde la colonne nullable)
-- Sinon, rendre la colonne NOT NULL
SET @null_count = (
    SELECT COUNT(*) 
    FROM depense_generale 
    WHERE ordonnateur_id IS NULL
);

SET @sql_final = IF(@null_count = 0,
    'ALTER TABLE depense_generale MODIFY COLUMN ordonnateur_id BIGINT NOT NULL',
    'SELECT "Il reste des NULL, colonne maintenue nullable" AS message'
);

PREPARE stmt_final FROM @sql_final;
EXECUTE stmt_final;
DEALLOCATE PREPARE stmt_final;

-- Étape 7: Vérifier et ajouter la contrainte de clé étrangère si elle n'existe pas
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'depense_generale' 
    AND CONSTRAINT_NAME = 'fk_depense_generale_ordonnateur'
);

SET @sql_fk = IF(@fk_exists = 0,
    'ALTER TABLE depense_generale ADD CONSTRAINT fk_depense_generale_ordonnateur FOREIGN KEY (ordonnateur_id) REFERENCES user(id) ON DELETE RESTRICT',
    'SELECT "Contrainte fk_depense_generale_ordonnateur existe déjà" AS message'
);

PREPARE stmt_fk FROM @sql_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

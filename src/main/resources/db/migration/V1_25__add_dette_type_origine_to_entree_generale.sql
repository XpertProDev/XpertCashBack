-- Type de la dette payée (ex. ECART_CAISSE) sur l'entrée de paiement pour afficher categorieNom sans requête supplémentaire
-- Idempotent : n'ajoute la colonne que si elle n'existe pas (évite #1060 si déjà appliqué)
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'entree_generale'
      AND COLUMN_NAME = 'dette_type_origine'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE entree_generale ADD COLUMN dette_type_origine VARCHAR(50) NULL COMMENT ''Type de la dette payée (ex. ECART_CAISSE), renseigné à l''''enregistrement du paiement''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Script de migration sécurisé pour ajouter le suivi des remboursements
-- Vérifie l'existence des colonnes avant de les ajouter

-- ============================================
-- 1. Migration sécurisée de la table vente_produit
-- ============================================

-- Ajouter quantite_remboursee si elle n'existe pas
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_NAME = 'vente_produit' 
     AND COLUMN_NAME = 'quantite_remboursee') = 0,
    'ALTER TABLE vente_produit ADD COLUMN quantite_remboursee INTEGER DEFAULT 0',
    'SELECT "Colonne quantite_remboursee existe déjà" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajouter montant_rembourse si elle n'existe pas
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_NAME = 'vente_produit' 
     AND COLUMN_NAME = 'montant_rembourse') = 0,
    'ALTER TABLE vente_produit ADD COLUMN montant_rembourse DOUBLE DEFAULT 0.0',
    'SELECT "Colonne montant_rembourse existe déjà" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajouter est_remboursee si elle n'existe pas
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_NAME = 'vente_produit' 
     AND COLUMN_NAME = 'est_remboursee') = 0,
    'ALTER TABLE vente_produit ADD COLUMN est_remboursee BOOLEAN DEFAULT FALSE',
    'SELECT "Colonne est_remboursee existe déjà" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 2. Migration sécurisée de la table vente
-- ============================================

-- Ajouter montant_total_rembourse si elle n'existe pas
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_NAME = 'vente' 
     AND COLUMN_NAME = 'montant_total_rembourse') = 0,
    'ALTER TABLE vente ADD COLUMN montant_total_rembourse DOUBLE DEFAULT 0.0',
    'SELECT "Colonne montant_total_rembourse existe déjà" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajouter date_dernier_remboursement si elle n'existe pas
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_NAME = 'vente' 
     AND COLUMN_NAME = 'date_dernier_remboursement') = 0,
    'ALTER TABLE vente ADD COLUMN date_dernier_remboursement DATETIME NULL',
    'SELECT "Colonne date_dernier_remboursement existe déjà" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajouter nombre_remboursements si elle n'existe pas
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_NAME = 'vente' 
     AND COLUMN_NAME = 'nombre_remboursements') = 0,
    'ALTER TABLE vente ADD COLUMN nombre_remboursements INTEGER DEFAULT 0',
    'SELECT "Colonne nombre_remboursements existe déjà" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 3. Mise à jour des données existantes
-- ============================================

-- Mettre à jour les enregistrements existants de vente_produit
UPDATE vente_produit 
SET est_remboursee = TRUE,
    quantite_remboursee = quantite
WHERE quantite = 0 
AND est_remboursee = FALSE;

-- Mettre à jour les enregistrements existants de vente
UPDATE vente v
SET montant_total_rembourse = (
    SELECT COALESCE(SUM(vh.montant), 0)
    FROM vente_historique vh
    WHERE vh.vente_id = v.id 
    AND vh.action = 'REMBOURSEMENT_VENTE'
),
nombre_remboursements = (
    SELECT COUNT(*)
    FROM vente_historique vh
    WHERE vh.vente_id = v.id 
    AND vh.action = 'REMBOURSEMENT_VENTE'
),
date_dernier_remboursement = (
    SELECT MAX(vh.date_action)
    FROM vente_historique vh
    WHERE vh.vente_id = v.id 
    AND vh.action = 'REMBOURSEMENT_VENTE'
)
WHERE EXISTS (
    SELECT 1 FROM vente_historique vh2 
    WHERE vh2.vente_id = v.id 
    AND vh2.action = 'REMBOURSEMENT_VENTE'
)
AND montant_total_rembourse = 0;

-- Script de migration simple et sécurisé
-- Ajoute seulement les colonnes manquantes

-- ============================================
-- 1. Vérifier et ajouter les colonnes manquantes pour vente_produit
-- ============================================

-- Ajouter montant_rembourse si elle n'existe pas
ALTER TABLE vente_produit 
ADD COLUMN IF NOT EXISTS montant_rembourse DOUBLE DEFAULT 0.0;

-- Ajouter est_remboursee si elle n'existe pas  
ALTER TABLE vente_produit 
ADD COLUMN IF NOT EXISTS est_remboursee BOOLEAN DEFAULT FALSE;

-- ============================================
-- 2. Vérifier et ajouter les colonnes manquantes pour vente
-- ============================================

-- Ajouter montant_total_rembourse si elle n'existe pas
ALTER TABLE vente ADD COLUMN IF NOT EXISTS montant_total_rembourse DOUBLE DEFAULT 0.0;

-- Ajouter date_dernier_remboursement si elle n'existe pas
ALTER TABLE vente ADD COLUMN IF NOT EXISTS date_dernier_remboursement DATETIME NULL;

-- Ajouter nombre_remboursements si elle n'existe pas
ALTER TABLE vente ADD COLUMN IF NOT EXISTS nombre_remboursements INTEGER DEFAULT 0;

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

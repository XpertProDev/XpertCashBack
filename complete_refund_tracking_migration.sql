-- Script de migration complet pour ajouter le suivi des remboursements
-- Tables: vente_produit et vente

-- ============================================
-- 1. Migration de la table vente_produit
-- ============================================

-- Ajouter les nouvelles colonnes à vente_produit
ALTER TABLE vente_produit 
ADD COLUMN quantite_remboursee INTEGER DEFAULT 0,
ADD COLUMN montant_rembourse DOUBLE DEFAULT 0.0,
ADD COLUMN est_remboursee BOOLEAN DEFAULT FALSE;

-- Mettre à jour les enregistrements existants de vente_produit
UPDATE vente_produit 
SET est_remboursee = TRUE,
    quantite_remboursee = quantite
WHERE quantite = 0;

-- ============================================
-- 2. Migration de la table vente
-- ============================================

-- Ajouter les nouvelles colonnes à vente
ALTER TABLE vente 
ADD COLUMN montant_total_rembourse DOUBLE DEFAULT 0.0,
ADD COLUMN date_dernier_remboursement DATETIME NULL,
ADD COLUMN nombre_remboursements INTEGER DEFAULT 0;

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
);

-- ============================================
-- 3. Commentaires sur les nouvelles colonnes
-- ============================================

-- Commentaires pour vente_produit
COMMENT ON COLUMN vente_produit.quantite_remboursee IS 'Quantité remboursée pour cette ligne de vente';
COMMENT ON COLUMN vente_produit.montant_rembourse IS 'Montant remboursé pour cette ligne de vente';
COMMENT ON COLUMN vente_produit.est_remboursee IS 'Indique si cette ligne est entièrement remboursée';

-- Commentaires pour vente
COMMENT ON COLUMN vente.montant_total_rembourse IS 'Montant total remboursé pour cette vente';
COMMENT ON COLUMN vente.date_dernier_remboursement IS 'Date du dernier remboursement effectué';
COMMENT ON COLUMN vente.nombre_remboursements IS 'Nombre de remboursements effectués pour cette vente';

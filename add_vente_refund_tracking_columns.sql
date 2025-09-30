-- Script de migration pour ajouter les colonnes de suivi de remboursement
-- à la table Vente

-- Ajouter les nouvelles colonnes
ALTER TABLE vente 
ADD COLUMN montant_total_rembourse DOUBLE DEFAULT 0.0,
ADD COLUMN date_dernier_remboursement DATETIME NULL,
ADD COLUMN nombre_remboursements INTEGER DEFAULT 0;

-- Mettre à jour les enregistrements existants
-- Calculer le montant total remboursé pour chaque vente basé sur l'historique
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

-- Commentaire sur les nouvelles colonnes
COMMENT ON COLUMN vente.montant_total_rembourse IS 'Montant total remboursé pour cette vente';
COMMENT ON COLUMN vente.date_dernier_remboursement IS 'Date du dernier remboursement effectué';
COMMENT ON COLUMN vente.nombre_remboursements IS 'Nombre de remboursements effectués pour cette vente';

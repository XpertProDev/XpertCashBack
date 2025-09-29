-- Script de migration pour ajouter les colonnes de suivi de remboursement
-- à la table VenteProduit

-- Ajouter les nouvelles colonnes
ALTER TABLE vente_produit 
ADD COLUMN quantite_remboursee INTEGER DEFAULT 0,
ADD COLUMN montant_rembourse DOUBLE DEFAULT 0.0,
ADD COLUMN est_remboursee BOOLEAN DEFAULT FALSE;

-- Mettre à jour les enregistrements existants
-- Pour les lignes où la quantité restante est 0, marquer comme remboursées
UPDATE vente_produit 
SET est_remboursee = TRUE,
    quantite_remboursee = quantite
WHERE quantite = 0;

-- Commentaire sur les nouvelles colonnes
COMMENT ON COLUMN vente_produit.quantite_remboursee IS 'Quantité remboursée pour cette ligne de vente';
COMMENT ON COLUMN vente_produit.montant_rembourse IS 'Montant remboursé pour cette ligne de vente';
COMMENT ON COLUMN vente_produit.est_remboursee IS 'Indique si cette ligne est entièrement remboursée';

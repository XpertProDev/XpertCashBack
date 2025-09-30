-- Script de migration Flyway pour ajouter les colonnes de suivi de remboursement
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

-- Ajout des champs pour lier les entrées de paiement aux dettes
ALTER TABLE entree_generale
    ADD COLUMN dette_id BIGINT NULL COMMENT 'ID de la dette payée (vente_id pour VENTE_CREDIT, entree_generale_id pour ENTREE_DETTE)',
    ADD COLUMN dette_type VARCHAR(50) NULL COMMENT 'Type de dette: VENTE_CREDIT ou ENTREE_DETTE',
    ADD COLUMN dette_numero VARCHAR(255) NULL COMMENT 'Numéro de référence de la dette (numéro facture ou numéro entrée)';


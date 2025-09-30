-- Script pour vérifier la structure des tables

-- Vérifier la structure de la table vente_produit
DESCRIBE vente_produit;

-- Vérifier la structure de la table vente
DESCRIBE vente;

-- Vérifier si les colonnes de remboursement existent déjà
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'vente_produit' 
AND COLUMN_NAME IN ('quantite_remboursee', 'montant_rembourse', 'est_remboursee');

SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'vente' 
AND COLUMN_NAME IN ('montant_total_rembourse', 'date_dernier_remboursement', 'nombre_remboursements');

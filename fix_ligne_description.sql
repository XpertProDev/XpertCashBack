-- Script SQL à exécuter directement dans MySQL pour corriger le problème
-- Connexion: mysql -u root -p xpertCash_db

-- Modifier la colonne ligne_description dans ligne_facture_proforma pour supporter des descriptions plus longues
ALTER TABLE ligne_facture_proforma 
    MODIFY COLUMN ligne_description TEXT;

-- Modifier également la colonne ligne_description dans ligne_facture_reelle pour cohérence
ALTER TABLE ligne_facture_reelle 
    MODIFY COLUMN ligne_description TEXT;

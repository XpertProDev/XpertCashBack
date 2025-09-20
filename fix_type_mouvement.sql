-- Script SQL pour modifier la colonne type_mouvement
ALTER TABLE mouvement_caisse MODIFY COLUMN type_mouvement VARCHAR(50);

-- Vérifier la structure actuelle
DESCRIBE mouvement_caisse;

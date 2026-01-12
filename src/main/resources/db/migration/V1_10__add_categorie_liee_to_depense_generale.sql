-- Ajouter la colonne categorie_liee_id pour stocker la catégorie liée (Categorie) pour CHARGE_VARIABLE
ALTER TABLE depense_generale
    ADD COLUMN categorie_liee_id BIGINT NULL,
    ADD CONSTRAINT fk_depense_generale_categorie_liee 
        FOREIGN KEY (categorie_liee_id) REFERENCES categorie(id) ON DELETE SET NULL;


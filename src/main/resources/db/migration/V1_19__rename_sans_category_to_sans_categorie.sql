-- Migration V1_19: Renommer les catégories "Sans Category" en "Sans Catégorie"
-- Aligne les données existantes avec le libellé utilisé dans ProduitService (getOrCreateSansCategory)

UPDATE categorie
SET nom = 'Sans Catégorie'
WHERE nom = 'Sans Category';

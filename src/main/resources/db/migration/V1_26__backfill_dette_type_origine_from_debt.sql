-- Remplir dette_type_origine pour les paiements déjà enregistrés avant la migration (ex. ENTREE_DETTE dont la dette payée est ECART_CAISSE)
UPDATE entree_generale p
INNER JOIN entree_generale d ON d.id = p.dette_id AND p.dette_type = 'ENTREE_DETTE'
SET p.dette_type_origine = d.dette_type
WHERE p.dette_type_origine IS NULL
  AND d.dette_type IS NOT NULL;

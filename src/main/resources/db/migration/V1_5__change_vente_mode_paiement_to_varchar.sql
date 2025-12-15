-- Rendre la colonne mode_paiement plus souple pour accepter toutes les valeurs de l'enum ModePaiement (dont CREDIT)
-- Si la colonne est déjà en VARCHAR, cette migration sera idempotente sur la plupart des bases.

ALTER TABLE vente
    MODIFY mode_paiement VARCHAR(50) NULL;



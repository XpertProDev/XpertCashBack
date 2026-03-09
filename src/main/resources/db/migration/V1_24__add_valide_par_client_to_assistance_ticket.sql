-- Ajoute un flag de validation par le client sur les tickets d'assistance.
-- Un ticket n'est définitivement clôturé que lorsque le client l'a validé.

ALTER TABLE assistance_ticket
    ADD COLUMN valide_par_client TINYINT(1) NOT NULL DEFAULT 0;


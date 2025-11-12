-- Script de migration Flyway pour ajouter la colonne numero à la table depense_generale
-- Format du numéro: DP: 001-11-2025 (généré automatiquement par l'application)

-- Ajouter la colonne numero si elle n'existe pas déjà
ALTER TABLE depense_generale 
ADD COLUMN IF NOT EXISTS numero VARCHAR(255) NULL;

-- Note: Les numéros seront générés automatiquement par l'application lors de la création
-- des nouvelles dépenses. Les dépenses existantes auront NULL comme numéro.


-- Adapter la colonne role.name pour supporter la nouvelle valeur d'énumération SUPPORT.
-- On remplace le type ENUM MySQL existant par un VARCHAR plus flexible.

ALTER TABLE role
    MODIFY COLUMN name VARCHAR(50) NOT NULL;


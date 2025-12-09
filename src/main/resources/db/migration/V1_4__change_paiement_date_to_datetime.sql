-- Script de migration Flyway pour changer datePaiement de DATE à DATETIME
-- Cela permet de stocker l'heure réelle du paiement

-- Modifier la colonne datePaiement de DATE à DATETIME
-- Pour MySQL/MariaDB
ALTER TABLE paiement 
MODIFY COLUMN date_paiement DATETIME NULL;

-- Note: Les paiements existants auront leur date convertie avec l'heure 00:00:00
-- Les nouveaux paiements auront l'heure réelle de création


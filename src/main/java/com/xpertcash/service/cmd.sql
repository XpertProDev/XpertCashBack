-- Vérifier les index de la table
SHOW INDEX FROM role;

-- Supprimer l'index unique qui bloque (UK8sewwnpamngi6b1dwaa88askk)
ALTER TABLE role DROP INDEX UK8sewwnpamngi6b1dwaa88askk;
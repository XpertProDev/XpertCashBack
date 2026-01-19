-- Script de diagnostic et correction des clients sans entreprise

-- 1. Vérifier les clients sans entreprise
SELECT 
    c.id, 
    c.nom_complet, 
    c.entreprise_id, 
    ec.id as entreprise_client_id,
    ec.entreprise_id as entreprise_client_entreprise_id
FROM client c
LEFT JOIN entreprise_client ec ON c.entreprise_client_id = ec.id
WHERE c.entreprise_id IS NULL 
   OR (c.entreprise_id IS NULL AND (ec.id IS NULL OR ec.entreprise_id IS NULL));

-- 2. Pour corriger : associer tous les clients sans entreprise à l'entreprise ID 2
-- ATTENTION: À adapter selon vos besoins
-- UPDATE client c
-- LEFT JOIN entreprise_client ec ON c.entreprise_client_id = ec.id
-- SET c.entreprise_id = 2
-- WHERE c.entreprise_id IS NULL 
--   AND (ec.id IS NULL OR ec.entreprise_id IS NULL OR ec.entreprise_id = 2);

-- 3. Vérifier tous les clients de l'entreprise 2
SELECT 
    c.id, 
    c.nom_complet, 
    c.entreprise_id,
    c.entreprise_client_id,
    ec.entreprise_id as entreprise_client_entreprise_id
FROM client c
LEFT JOIN entreprise_client ec ON c.entreprise_client_id = ec.id
WHERE c.entreprise_id = 2
   OR ec.entreprise_id = 2;

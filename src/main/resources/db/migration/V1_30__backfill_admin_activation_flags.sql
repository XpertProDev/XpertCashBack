-- Corrige les comptes ADMIN existants qui restent non activés après migration.
-- Objectif:
-- 1) activer tous les ADMIN d'entreprise (activated_lien=1, enabled_lien=1)
-- 2) réactiver les utilisateurs de leurs entreprises (enabled_lien=1),
--    comme le fait déjà la logique applicative lors d'une activation admin.

UPDATE `user` u
INNER JOIN role r ON r.id = u.role_id
SET u.activated_lien = 1,
    u.enabled_lien = 1
WHERE UPPER(r.name) = 'ADMIN'
  AND (u.activated_lien = 0 OR u.enabled_lien = 0);

UPDATE `user` u
INNER JOIN entreprise e ON e.id = u.entreprise_id
INNER JOIN `user` a ON a.id = e.admin_id
INNER JOIN role ra ON ra.id = a.role_id
SET u.enabled_lien = 1
WHERE UPPER(ra.name) = 'ADMIN'
  AND a.activated_lien = 1
  AND u.enabled_lien = 0;

-- Suppression de tout ce qui est lié au rôle SUPPORT : utilisateurs, dépendances, puis le rôle.
-- À exécuter après V1_28 si des comptes SUPPORT ont été recréés par l'application.
-- Id du rôle SUPPORT (NULL si déjà supprimé ; les DELETE n'impactent alors aucune ligne)
SET @support_role_id = (SELECT id FROM role WHERE name = 'SUPPORT' LIMIT 1);
DELETE FROM user_sessions WHERE user_id IN (SELECT id FROM `user` WHERE role_id = @support_role_id);
DELETE FROM initial_password_token WHERE user_id IN (SELECT id FROM `user` WHERE role_id = @support_role_id);
DELETE FROM password_reset_token WHERE user_id IN (SELECT id FROM `user` WHERE role_id = @support_role_id);
DELETE FROM global_notifications WHERE user_id IN (SELECT id FROM `user` WHERE role_id = @support_role_id);
DELETE FROM user_boutique WHERE user_id IN (SELECT id FROM `user` WHERE role_id = @support_role_id);
UPDATE entreprise e INNER JOIN `user` u ON e.admin_id = u.id AND u.role_id = @support_role_id SET e.admin_id = NULL;
DELETE FROM `user` WHERE role_id = @support_role_id;
DELETE FROM role WHERE name = 'SUPPORT';

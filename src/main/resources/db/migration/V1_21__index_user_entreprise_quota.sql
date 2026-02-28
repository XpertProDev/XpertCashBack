-- Index pour les requêtes SaaS par tenant : count utilisateurs et déblocage par quota.
-- Réduit le coût des requêtes par entreprise_id et locked_by_quota (liste entreprises, unlock).
CREATE INDEX idx_user_entreprise_locked_by_quota ON `user` (entreprise_id, locked_by_quota);

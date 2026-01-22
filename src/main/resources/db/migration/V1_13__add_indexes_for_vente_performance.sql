-- Migration V1_13: Ajout d'index pour optimiser les requêtes de statistiques de vente
-- Ces index améliorent considérablement les performances des requêtes sur les ventes et produits vendus
-- Essentiel pour gérer de grandes quantités de données et de nombreux utilisateurs simultanés

-- Index sur les clés étrangères de vente_produit pour optimiser les JOINs
CREATE INDEX IF NOT EXISTS idx_vente_produit_vente_id ON vente_produit(vente_id);
CREATE INDEX IF NOT EXISTS idx_vente_produit_produit_id ON vente_produit(produit_id);

-- Index sur les clés étrangères de vente pour optimiser les JOINs
CREATE INDEX IF NOT EXISTS idx_vente_boutique_id ON vente(boutique_id);
CREATE INDEX IF NOT EXISTS idx_vente_vendeur_id ON vente(vendeur_id);
CREATE INDEX IF NOT EXISTS idx_vente_caisse_id ON vente(caisse_id);

-- Index composite pour optimiser les requêtes de statistiques par entreprise
-- Permet de filtrer rapidement par entreprise et exclure les produits supprimés
CREATE INDEX IF NOT EXISTS idx_produit_deleted ON produit(deleted);

-- Index sur les champs utilisés dans les calculs de remboursement
CREATE INDEX IF NOT EXISTS idx_vente_produit_est_remboursee ON vente_produit(est_remboursee);

-- Index composite pour optimiser les requêtes GROUP BY sur vente_produit
-- Améliore les performances des agrégations (SUM, COUNT) par produit
CREATE INDEX IF NOT EXISTS idx_vente_produit_produit_rembourse ON vente_produit(produit_id, est_remboursee);

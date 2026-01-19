-- Migration V1_12: Ajout d'index pour optimiser les performances des requêtes isolées par entreprise
-- Ces index améliorent considérablement les performances des requêtes filtrées par entreprise_id

-- Index sur les tables principales pour les requêtes par entreprise
CREATE INDEX IF NOT EXISTS idx_facture_proforma_entreprise_id ON facture_pro_forma(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_facture_reelle_entreprise_id ON facture_reelle(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_client_entreprise_id ON client(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_entreprise_client_entreprise_id ON entreprise_client(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_boutique_entreprise_id ON boutique(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_produit_boutique_id ON produit(boutique_id);
CREATE INDEX IF NOT EXISTS idx_user_entreprise_id ON user(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_categorie_entreprise_id ON categorie(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_unite_entreprise_id ON unite(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_categorie_depense_entreprise_id ON categorie_depense(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_depense_generale_entreprise_id ON depense_generale(entreprise_id);
CREATE INDEX IF NOT EXISTS idx_entree_generale_entreprise_id ON entree_generale(entreprise_id);

-- Index composites pour les requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_facture_proforma_client_entreprise ON facture_pro_forma(client_id, entreprise_id);
CREATE INDEX IF NOT EXISTS idx_facture_proforma_statut_entreprise ON facture_pro_forma(statut, entreprise_id);
CREATE INDEX IF NOT EXISTS idx_user_email_entreprise ON user(email, entreprise_id);
CREATE INDEX IF NOT EXISTS idx_user_phone_entreprise ON user(phone, entreprise_id);
CREATE INDEX IF NOT EXISTS idx_produit_categorie_entreprise ON produit(categorie_id, boutique_id);

-- Index pour les recherches par date et entreprise (factures)
CREATE INDEX IF NOT EXISTS idx_facture_proforma_date_entreprise ON facture_pro_forma(date_creation, entreprise_id);
CREATE INDEX IF NOT EXISTS idx_facture_reelle_date_entreprise ON facture_reelle(date_creation, entreprise_id);

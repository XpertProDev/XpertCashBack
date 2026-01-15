# Guide de Migration en Production

## 📋 Résumé des Changements

Suite aux optimisations pour rendre le système optimal pour un SaaS avec isolation des données par entreprise, voici ce qui doit être fait en production.

---

## ✅ 1. MIGRATION OBLIGATOIRE - V1_11

### Description
Modification du type de colonne `ligne_description` de `VARCHAR` à `TEXT` pour permettre des descriptions plus longues dans les lignes de factures.

### Fichier
`src/main/resources/db/migration/V1_11__modify_ligne_description_to_text.sql`

### À faire
**Si la migration n'a pas encore été appliquée en production :**

```sql
-- Exécuter manuellement sur la base de données de production
ALTER TABLE ligne_facture_proforma 
    MODIFY COLUMN ligne_description TEXT;

ALTER TABLE ligne_facture_reelle 
    MODIFY COLUMN ligne_description TEXT;
```

**⚠️ IMPORTANT :**
- Cette migration est **sans risque** (changement de type compatible)
- Pas de perte de données
- Pas de downtime requis
- Peut être exécutée à tout moment

---

## ⚡ 2. MIGRATION OPTIONNELLE - V1_12 (Recommandée)

### Description
Ajout d'index sur les colonnes `entreprise_id` et autres colonnes fréquemment utilisées pour **améliorer drastiquement les performances** des requêtes isolées par entreprise.

### Fichier
`src/main/resources/db/migration/V1_12__add_indexes_for_performance.sql`

### Avantages
- ✅ **Performance** : Requêtes 10-100x plus rapides sur les tables volumineuses
- ✅ **Scalabilité** : Meilleure gestion de la croissance des données
- ✅ **Expérience utilisateur** : Temps de réponse réduits

### À faire
```bash
# Exécuter la migration si vous avez Flyway configuré
# Sinon, exécuter manuellement le script SQL sur la base de production
```

**⚠️ NOTE :**
- Les index prennent un peu d'espace disque supplémentaire
- L'insertion peut être légèrement ralentie (négligeable)
- Les requêtes SELECT seront **beaucoup plus rapides**

---

## 🔄 3. CHANGEMENTS DE CODE - PAS DE MIGRATION NÉCESSAIRE

### Ce qui a été fait
- ✅ Ajout de nouvelles méthodes dans les repositories (isolation par `entrepriseId`)
- ✅ Optimisation des requêtes JPQL avec `JOIN FETCH`
- ✅ Suppression de méthodes dépréciées
- ✅ Modification des services pour utiliser les nouvelles méthodes isolées

### Impact
- ✅ **Aucune migration nécessaire** pour ces changements
- ✅ **Rétrocompatibilité** : Les anciennes méthodes ne sont utilisées que dans des cas marginaux
- ✅ **Déploiement** : Simple redémarrage de l'application

---

## 🚀 Procédure de Déploiement

### Option 1 : Avec Flyway (Recommandé)
Si Flyway est configuré, les migrations seront appliquées automatiquement au démarrage :

```bash
# 1. Vérifier que les migrations sont dans le dossier
ls src/main/resources/db/migration/

# 2. Déployer l'application
# Les migrations V1_11 et V1_12 seront appliquées automatiquement
```

### Option 2 : Migration Manuelle
Si vous utilisez `ddl-auto=update` (actuellement en production) :

```bash
# 1. Appliquer la migration V1_11 (OBLIGATOIRE)
mysql -u xpert_db -p xpertCash_db < src/main/resources/db/migration/V1_11__modify_ligne_description_to_text.sql

# 2. Appliquer la migration V1_12 (OPTIONNELLE mais recommandée)
mysql -u xpert_db -p xpertCash_db < src/main/resources/db/migration/V1_12__add_indexes_for_performance.sql

# 3. Redémarrer l'application
```

---

## 📊 Vérification Post-Migration

### Vérifier que V1_11 est appliquée
```sql
DESCRIBE ligne_facture_proforma;
-- La colonne ligne_description doit être de type TEXT
```

### Vérifier que V1_12 est appliquée
```sql
SHOW INDEX FROM facture_proforma;
SHOW INDEX FROM client;
-- Vous devriez voir les nouveaux index avec entreprise_id
```

---

## ⚠️ Points d'Attention

1. **Backup** : Toujours faire un backup avant toute migration en production
2. **Test** : Tester d'abord en environnement de staging
3. **Monitoring** : Surveiller les logs après déploiement
4. **Downtime** : Aucun downtime requis pour ces migrations

---

## 🎯 Résumé Rapide

| Migration | Type | Priorité | Risque |
|-----------|------|----------|--------|
| V1_11 | Obligatoire | 🔴 Haute | 🟢 Faible |
| V1_12 | Optionnelle | 🟡 Moyenne | 🟢 Faible |

**Conclusion :** 
- ✅ **Migration V1_11 obligatoire** (5 minutes)
- ⚡ **Migration V1_12 recommandée** pour les performances (10 minutes)
- ✅ **Redémarrage de l'application** pour appliquer les changements de code

---

## 📞 Support

En cas de problème pendant la migration, restaurer le backup et contacter l'équipe technique.

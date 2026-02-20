# Analyse approfondie – XpertCashBack & préparation SaaS multi-tenant

## 1. Vue d’ensemble de l’architecture

| Élément | Détail |
|--------|--------|
| **Stack** | Spring Boot 3.4.2, Java 21, Maven |
| **Base de données** | MySQL (`xpertCash_db`), une instance, un schéma |
| **Modèle multi-tenant** | **Une base, un schéma** ; le tenant = **Entreprise** (isolation par ligne via `entreprise_id` ou `boutique.entreprise_id`) |
| **Authentification** | JWT (Bearer), résolution user → entreprise dans les services |
| **Sécurité HTTP** | `anyRequest().permitAll()` ; pas de règles par URL, tout est géré en couche service |

---

## 2. Structure du projet

- **configuration** : Security, JWT, CORS, WebMvc
- **controller** : REST sous `/api/auth` (entreprise, users, produits, factures, ventes, trésorerie, comptabilité, etc.)
- **service** : logique métier (dont VENTE, PROSPECT, Module, PASSWORD)
- **repository** : JPA, avec méthodes du type `findByEntrepriseId`, `findByBoutique_Entreprise_Id`, etc.
- **entity** : JPA (Entreprise, User, Boutique, Produit, Vente, Facture*, DepenseGenerale, EntreeGenerale, etc.)
- **composant** : Utilitaire, SuperAdminInitializer, AuthorizationService

Les données métier (utilisateurs, produits, factures, ventes, trésorerie, comptabilité) sont rattachées à **Entreprise** directement ou via **Boutique**.

---

## 3. Modèle de données et isolation par tenant

### 3.1 Racine tenant : `Entreprise`

- **Entreprise** : id, nom, identifiant, email, pays, secteur, logo, admin, modulesActifs, etc.
- **User** : `entreprise_id`, `role_id` → chaque utilisateur appartient à une entreprise.
- **Boutique** : `entreprise_id` → ventes, produits, caisses, transferts sont liés à une entreprise via la boutique.
- **FactureReelle / FactureProForma** : `entreprise_id`.
- **DepenseGenerale / EntreeGenerale** : `entreprise_id`.
- **Client / Fournisseur** : liés à l’entreprise.

L’isolation est donc **row-level** par `entreprise_id` (ou par `boutique.entreprise.id`), pas par schéma ni par base.

### 3.2 Mécanisme d’isolation actuel

- **JWT** : identité utilisateur (UUID) ; pas d’`entrepriseId` dans le token.
- **Chaque requête** : le service récupère l’utilisateur via `AuthenticationHelper` / `Utilitaire.getAuthenticatedUser(request)` puis `user.getEntreprise().getId()` et utilise cet `entrepriseId` dans les appels repository.
- **Pas de** `TenantContext`, filtre HTTP ou filtre Hibernate : le tenant est recalculé à chaque appel.
- **Contrôles d’accès** : `CentralAccess` (isAdminOfEntreprise, isAdminOrManagerOfEntreprise, isComptable, isSelfOrAdminOrManager) utilisés dans les services pour trésorerie, comptabilité, etc.

En l’état, le modèle est **conçu** pour du multi-tenant (une entreprise = un tenant), mais l’isolation dépend entièrement du fait que **chaque endpoint** reçoive la requête, résolve l’utilisateur et filtre par `entrepriseId`. Toute omission crée un risque de fuite de données.

---

## 4. Points forts pour un SaaS multi-tenant

1. **Modèle métier cohérent** : Entreprise au centre ; utilisateurs, produits, factures, ventes, trésorerie, comptabilité bien reliés à l’entreprise (ou à la boutique).
2. **Repositories** : Beaucoup de méthodes déjà scopées par entreprise (`findByEntrepriseId`, `findByBoutique_Entreprise_Id`, etc.).
3. **Pagination** : Présente sur des listes critiques (FactureReelle, FactureVente, Produit par boutique, Caisse, liste entreprises SuperAdmin).
4. **Pool et concurrence** : HikariCP (max 100, min idle 20) et Tomcat (max 200 threads, max-connections 10000) configurés pour du multi-thread / multi-entreprises.
5. **Hibernate** : batch_size, order_inserts/updates, fetch_size configurés (prod).
6. **SuperAdmin** : Rôle dédié, liste paginée d’entreprises (hors compte technique plateforme), désactivation entreprise, stats dashboard ; pas de mélange avec les utilisateurs “normaux”.

Donc, **sur le papier**, l’architecture peut supporter beaucoup d’entreprises et d’utilisateurs, **à condition** de corriger les failles d’isolation et de sécurité ci-dessous.

---

## 5. Problèmes critiques (à corriger avant mise en production SaaS)

### 5.1 Fuite de données : liste de toutes les ventes

- **Fichier** : `VenteController.java` (ligne ~80), `VenteService.java` (ligne ~561).
- **Endpoint** : `GET /api/auth/vente`
- **Comportement** : Aucun `HttpServletRequest`, aucun filtre par entreprise. `venteRepository.findAll()` renvoie **toutes les ventes de toutes les entreprises**.
- **Impact** : Un utilisateur authentifié (n’importe quelle entreprise) peut voir toutes les ventes du système.
- **Action** : Supprimer cet endpoint ou le remplacer par une liste **obligatoirement** scopée par l’entreprise de l’utilisateur (avec `HttpServletRequest` + pagination par `entrepriseId`).

### 5.2 Fuite de données : liste de tous les transferts

- **Fichier** : `BoutiqueController.java` (ligne ~249).
- **Endpoint** : `GET /api/auth/transferts?boutiqueId=`
- **Comportement** : Si `boutiqueId` est absent, `transfertRepository.findAll()` est appelé. Aucune vérification utilisateur/entreprise.
- **Impact** : Un utilisateur authentifié peut obtenir **tous les transferts de toutes les entreprises**.
- **Action** : Exiger soit un `boutiqueId` validé (appartenant à l’entreprise de l’utilisateur), soit dériver l’entreprise du JWT et ne lister que les transferts de cette entreprise (sans jamais faire `findAll()` sans filtre tenant).

### 5.3 Sécurité HTTP trop permissive

- **Fichier** : `SecurityConfig.java`
- **Comportement** : `anyRequest().permitAll()` : toutes les URLs sont autorisées au niveau du filtre Spring Security.
- **Impact** : Toute nouvelle API sous `/api/auth/**` est exposée sans contrôle au niveau HTTP ; si un développeur oublie de vérifier le JWT et l’entreprise en service, la donnée peut fuiter.
- **Action** : Au minimum, exiger une authentification pour `/api/auth/**` (par exemple `authenticated()`) et laisser la granularité (rôles, entreprise) en couche service. Cela évite les appels “sans token” et renforce la discipline.

### 5.4 Performance / scalabilité : filtrage en mémoire

- **Fichier** : `ProduitService.java` (historique stock et liste des stocks).
- **Comportement** : `stockHistoryRepository.findAll()` puis `stream().filter(...)` par `entrepriseId` ; idem pour `stockRepository.findAll()`.
- **Impact** : Avec beaucoup d’entreprises et de données, cela charge toute la table en mémoire puis filtre. Non scalable.
- **Action** : Introduire des méthodes repository scopées par entreprise (ex. par `boutique.entreprise.id` ou jointure appropriée) et pagination côté base, sans `findAll()` sur des tables partagées.

---

## 6. Autres points d’attention

1. **Pas de TenantContext** : Chaque service doit penser à récupérer l’utilisateur et l’`entrepriseId`. Risque d’oubli sur de nouveaux endpoints. Une approche optionnelle : filtre ou interceptor qui pose un `TenantContext` (thread-local) à partir du JWT pour centraliser et éviter les oublis.
2. **Migrations** : Des scripts type Flyway existent sous `db/migration/` mais Flyway n’est pas dans le `pom.xml` ; le schéma est géré par Hibernate (`ddl-auto=update` en dev). Pour la prod et l’évolution du schéma, des migrations versionnées (ex. Flyway) et des index explicites sur `entreprise_id` (et colonnes de jointure) sont recommandés.
3. **Index base de données** : Pour des requêtes du type `WHERE entreprise_id = ?` et jointures `boutique -> entreprise`, des index sur `entreprise_id` (et clés étrangères) sont nécessaires pour tenir la charge avec beaucoup d’entreprises. À formaliser dans des scripts de migration.
4. **Listes non paginées** : Plusieurs listes (ventes par boutique, par vendeur, etc.) renvoient toute la liste. À terme, pour des grosses boutiques, il faudra pagination et filtres pour éviter des réponses énormes.

---

## 7. Synthèse : prêt pour un SaaS multi-tenant à forte charge ?

| Critère | État |
|--------|------|
| Modèle multi-tenant (Entreprise = tenant) | OK |
| Données métier (users, produits, factures, ventes, trésorerie, comptabilité) liées à l’entreprise | OK |
| Isolation par entreprise dans la majorité des services | OK |
| Pagination sur certaines listes critiques | OK |
| Pool de connexions et réglages multi-thread | OK |
| Fuite de données (ventes, transferts) | **À corriger** |
| Sécurité HTTP (anyRequest().permitAll()) | **À durcir** |
| Filtrage en mémoire (stock / historique) | **À remplacer par requêtes scopées + pagination** |
| Migrations et index explicites | Recommandé |

**Verdict** : Le système est **en grande partie prêt** pour un SaaS multi-tenant avec beaucoup d’entreprises et d’utilisateurs (produits, factures, ventes, trésorerie, comptabilité) **à condition** de :

1. **Corriger immédiatement** les deux fuites de données (liste ventes, liste transferts sans filtre entreprise).
2. **Renforcer** la sécurité HTTP (exiger authentification sur `/api/auth/**`).
3. **Remplacer** les `findAll()` + filtre en mémoire par des requêtes scopées par entreprise et paginées.
4. **Prévoir** des migrations versionnées et des index sur `entreprise_id` (et clés liées) pour la montée en charge.

Après ces corrections, l’architecture peut **supporter un SaaS multi-tenant avec beaucoup d’entreprises et leurs utilisateurs, produits, factures, ventes, trésorerie et comptabilité**, avec un suivi rigoureux des nouveaux endpoints (toujours passer par le user authentifié et l’`entrepriseId`).

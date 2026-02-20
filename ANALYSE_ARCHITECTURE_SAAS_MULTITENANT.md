# Analyse XpertCashBack – Préparation SaaS multi-tenant

Document d’analyse de l’architecture du système XpertCashBack pour évaluer sa capacité à supporter un **SaaS multi-tenant** avec de nombreuses entreprises et leurs utilisateurs, boutiques, produits, factures, stock, ventes, factures de vente, trésorerie, comptabilité, dépenses, etc.

---

## 1. Structure du projet

**Dossiers principaux sous `src/main/java/com/xpertcash/` :**

| Dossier           | Rôle                                                                 |
|-------------------|----------------------------------------------------------------------|
| `controller/`     | Contrôleurs REST (VENTE/, PROSPECT/, Module/, PASSWORD/, Files/)     |
| `service/`        | Services métier (VENTE/, PROSPECT/, Module/, IMAGES/, PASSWORD/)     |
| `repository/`     | Repositories JPA (VENTE/, PROSPECT/, Module/, PASSWORD/)             |
| `entity/`         | Entités JPA (Enum/, PROSPECT/, VENTE/, Module/, PASSWORD/)           |
| `DTOs/`           | DTOs par domaine (USER, VENTE, PROSPECT, etc.)                       |
| `configuration/`  | JwtUtil, JwtConfig, SecurityConfig, WebSocketConfig, etc.             |
| `composant/`      | AuthorizationService, Utilitaire                                     |
| `exceptions/`     | Exceptions métier                                                    |

**Inventaire :**
- **Controllers** : ~38 classes `*Controller.java` (base path majoritairement `/api/auth`).
- **Services** : ~40 classes `*Service.java`.
- **Repositories** : ~48 interfaces `*Repository.java` (JpaRepository).
- **Entities** : ~45 classes `@Entity`.

---

## 2. Multi-tenancy – Modèle actuel

**Stratégie : une base partagée, identifiant tenant = `Entreprise` (id / `entreprise_id`).**

- Pas de `tenant_id` générique, ni `organisation_id`, ni schéma par tenant.
- **Clé de partition** : entité `Entreprise` avec `id` (Long). Les entités métier sont liées via `@ManyToOne Entreprise` ou via une chaîne (ex. `Boutique` → `Entreprise`).

**Où c’est utilisé :**
- **Entities** : `entreprise_id` ou relation `Entreprise` sur User, Boutique, Categorie, CategorieDepense, Client, DepenseGenerale, EntreeGenerale, FactureProForma, FactureReelle, Fournisseur, Unite, EntrepriseClient, TransfertFonds, Prospect, et entités Module. Produit passe par `Produit.boutique.entreprise`. Vente, Caisse, Stock passent par Boutique → Entreprise.
- **Repositories** : nombreuses méthodes `findByEntrepriseId`, `findByIdAndEntrepriseId`, `findBy...AndEntrepriseId` (voir section 5).
- **Services** : le contexte entreprise est dérivé de l’utilisateur connecté (`user.getEntreprise().getId()`) après résolution via JWT (AuthenticationHelper), puis passé aux repositories.
- **Filtres JPA globaux** : **aucun** `@Filter` / `FilterDef` / `enableFilter` Hibernate ; l’isolation est faite manuellement dans les requêtes et services.

---

## 3. Controllers – Contexte tenant

Tous les controllers sont en `@RestController` sous `/api/auth` (sauf exceptions). Le contexte tenant est obtenu via **JWT** → `AuthenticationHelper.getAuthenticatedUser(request)` → `user.getEntreprise()`.

| Domaine              | Controller(s) principal(aux)        | Contexte tenant                                      |
|----------------------|-------------------------------------|------------------------------------------------------|
| Super Admin          | SuperAdminController                | Pas d’entreprise (gestion globale)                  |
| Factures proforma    | FactureProformaController           | Via authHelper + entreprise de l’user                |
| Factures réelles     | FactureReelleController            | Idem                                                 |
| Factures (achats)    | facturesController                  | **Exemple correct** : authHelper + `findBy*EntrepriseId` |
| Vente / caisse       | FactureVenteController, CaisseController, VenteController | Idem                          |
| Trésorerie           | TresorerieController                | Via `validerEntrepriseEtPermissions`                 |
| Comptabilité         | ComptabiliteController              | Via authHelper + repositories filtrés                |
| Produits / stock     | ProduitController, StockController  | Idem ; commentaire « isolation multi-tenant »       |
| Boutiques            | BoutiqueController                  | Idem ; isolation sur transferts                      |
| Utilisateurs         | UsersController                     | Entreprise de l’user (path `entrepriseId` non utilisé) |
| Clients / fournisseurs | ClientController, FournisseurController | Vérification entreprise quand path contient id   |
| Autres               | Prospect, TransfertFonds, Alertes, etc. | Contexte dérivé du JWT                            |

**Point d’attention :** pas de header `X-Tenant-Id` ni tenant dans le path (sauf quelques endpoints avec `entrepriseId`, vérifié côté service). L’entreprise n’est jamais prise uniquement depuis l’URL sans contrôle.

---

## 4. Services – Usage entreprise/tenant

**Services qui utilisent explicitement l’entreprise (user.getEntreprise() ou paramètre `entrepriseId`) :**

- **FactureVenteService**, **FactureProformaService**, **FactureReelleService** : requêtes par `entrepriseId`, vérifications vendeur/boutique dans l’entreprise.
- **TresorerieService** : `validerEntrepriseEtPermissions(request)` puis tout le calcul par `entrepriseId`.
- **ComptabiliteService** : authHelper + repositories filtrés par entreprise (dépenses, entrées, catégories).
- **ProduitService**, **BoutiqueService**, **CategorieService**, **UniteService** : travail par entreprise de l’user.
- **ClientService** : `getClientsByEntreprise(entrepriseId, request)` avec **vérification** que `entrepriseId` = `user.getEntreprise().getId()`.
- **FournisseurService**, **ProspectService**, **EntrepriseClientService** : authHelper + filtrage par entreprise.
- **TransfertFondsService** : liste des transferts de l’entreprise (isolation multi-tenant).
- **ModuleActivationService**, **EntrepriseService**, **UsersService** (entreprise de l’user pour liste des users).

**Services critiques (factures, stock, vente, trésorerie, comptabilité, dépenses) :** tous utilisent l’entreprise de l’user et des repositories filtrés par `entrepriseId`. Le **StockService** n’a pas de filtre entreprise direct ; l’isolation repose sur Produit/Boutique déjà contrôlés en amont.

---

## 5. Repositories – Filtrage par tenant

**Repositories avec méthodes explicites par entreprise (extraits) :**

- UsersRepository, BoutiqueRepository, CategorieRepository, CategorieDepenseRepository, ProduitRepository, StockRepository.
- DepenseGeneraleRepository, EntreeGeneraleRepository.
- FactureProformaRepository, FactureReelleRepository, FactureVenteRepository, FactureRepository.
- VenteRepository, CaisseRepository, MouvementCaisseRepository, VersementComptableRepository.
- ClientRepository, FournisseurRepository, EntrepriseClientRepository, ProspectRepository.
- TransfertFondsRepository, PaiementRepository, UniteRepository, UserBoutiqueRepository.
- NoteFactureProFormaRepository, FactProHistoriqueActionRepository, ComptabilitePaginationRepository.
- Module : EntrepriseModuleEssaiRepository, EntrepriseModuleAbonnementRepository, PaiementModuleRepository.

**Filtre JPA global (Hibernate @Filter) :** **aucun**. L’isolation repose uniquement sur les méthodes de repository et les `@Query` incluant `entreprise_id` ou `entreprise.id`.

**Risque :** tout appel direct à `findById(id)` sans vérification entreprise peut retourner une entité d’un autre tenant si l’ID est deviné.

---

## 6. Sécurité et contexte utilisateur / tenant

**Mécanisme :**
- **JWT** dans l’en-tête `Authorization: Bearer <token>`.
- **AuthenticationHelper** : lit le token, extrait l’UUID utilisateur (ou ID legacy), vérifie optionnellement la session (UserSession), charge l’utilisateur via `UsersRepository.findByUuid(userUuid)`.
- L’**entreprise** n’est pas dans le token : elle vient de **User.entreprise** (`user.getEntreprise()`). Donc tenant = `user.getEntreprise().getId()`.
- Pas de `TenantContext` ou thread-local explicite ; le contexte tenant est porté par l’instance `User` et recalculé à chaque appel.
- Contrôle d’accès métier : `CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId)`, rôles et permissions (RoleType, PermissionType).

**Fichiers clés :**
- `service/AuthenticationHelper.java` : `getAuthenticatedUser(request)`, `getAuthenticatedUserWithFallback(request)`.
- `configuration/JwtUtil.java` : extraction des claims depuis le JWT.

---

## 7. Base de données – Entités principales et lien tenant

| Entité           | Lien tenant                                      |
|------------------|--------------------------------------------------|
| Entreprise       | Racine tenant (id)                               |
| User             | `entreprise_id` → Entreprise                     |
| Boutique         | `entreprise_id` → Entreprise                     |
| Produit          | Via `boutique_id` → Boutique → Entreprise        |
| Stock            | Via Produit / Boutique → Entreprise              |
| FactureProForma, FactureReelle | `entreprise_id`                    |
| Facture (achats) | Via Boutique → Entreprise (FactureRepository)   |
| FactureVente, Vente | Via Boutique → Entreprise (pas d’entreprise_id direct) |
| Caisse           | Via Boutique → Entreprise                        |
| DepenseGenerale, EntreeGenerale | `entreprise_id`                 |
| Client, EntrepriseClient | `entreprise_id`                         |
| Categorie, CategorieDepense, Fournisseur, Unite | `entreprise_id`     |
| TransfertFonds, Prospect, Paiement, Modules | Référence Entreprise        |

Modèle cohérent : une entreprise, N boutiques, N users, N produits par boutique, ventes/caisses par boutique, dépenses/entrées/factures par entreprise. Pas de schéma par tenant ni base par tenant.

---

## 8. Audit des usages `findById` à risque

Les appels suivants utilisent `findById(id)` **sans** filtre explicite par `entrepriseId`. Ils peuvent exposer des données d’un autre tenant si l’appelant fournit un ID d’une autre entreprise. À auditer et, selon le cas, remplacer par `findByIdAndEntrepriseId` ou vérifier l’entreprise après chargement.

**À corriger en priorité (données sensibles par entreprise) :**
- **FactureProformaController** : `factureProformaRepository.findById(id)` (l.212).
- **FactureReelleService** : `factureReelleRepository.findById(factureId)` (plusieurs occurrences).
- **FactProHistoriqueService** : `factureProformaRepository.findById(factureId)` (l.59).
- **ClientService** : `clientRepository.findById(id)` (l.198), `clientRepository.findById(clientId)` (l.410), et usages similaires.
- **FournisseurController** : `fournisseurRepository.findById(fournisseurId)` (l.237).
- **EntrepriseClientService** : `entrepriseClientRepository.findById(id)` (l.115, 170, 200).
- **ComptabiliteService** : `venteRepository.findById`, `entreeGeneraleRepository.findById`, `produitRepository.findById` (vérifier que l’appel est toujours après contrôle entreprise).
- **VenteService** : `venteRepository.findById`, `clientRepository.findById`, `entrepriseClientRepository.findById` (vérifier que boutique/client sont bien de l’entreprise).
- **FactureProformaService** : `factureProformaRepository.findById(factureId)` (plusieurs fois) ; idem pour Client, EntrepriseClient, Produit (vérifier que les IDs viennent de listes déjà filtrées par entreprise).
- **CategorieService** : `categorieRepository.findById(categorieId)` (l.352).
- **GlobalNotificationService** : `notificationRepo.findById(notificationId)` (vérifier si les notifications sont scopées par entreprise).

**Cas où le risque est limité (entité déjà contrôlée ou usage interne) :**
- **BoutiqueService**, **CaisseService**, **ProduitService**, **Utilitaire** : `boutiqueRepository.findById(boutiqueId)` — à sécuriser si l’`boutiqueId` peut venir de l’URL sans vérification préalable (idéalement utiliser `findByIdAndEntrepriseId` après résolution de l’entreprise).
- **UsersService**, **RoleService**, **StockService**, etc. : `usersRepository.findById(userId)` — à vérifier selon le flux (user de la même entreprise ou non).
- **EntrepriseService**, **SuperAdminService** : accès entreprise par id — cohérent avec leur rôle (admin entreprise / super-admin).

**Exemple de bon pattern (à généraliser) :**  
`facturesController` : utilise `authHelper.getAuthenticatedUserWithFallback(request)`, `user.getEntreprise()`, puis `factureRepository.findAllByEntrepriseIdPaginated(entreprise.getId(), pageable)` et `boutiqueRepository.findByIdAndEntrepriseId(boutiqueId, entreprise.getId())` pour les factures par boutique.

---

## 9. Synthèse – Prêt pour un SaaS multi-tenant ?

### Points forts

- **Modèle métier** déjà orienté entreprise : la plupart des entités ont une FK ou un chemin vers `Entreprise`, avec contraintes (ex. unique (nom, entreprise_id)).
- **Contexte tenant dérivé du JWT** : l’entreprise vient de l’utilisateur authentifié, ce qui limite le « tenant switching » par l’appelant.
- **Vérifications explicites** quand l’URL contient un `entrepriseId` (ex. ClientService).
- **Repositories** largement préparés : nombreuses méthodes `findBy...EntrepriseId` / `findByIdAndEntrepriseId`.
- **Services métiers critiques** (factures, trésorerie, comptabilité, ventes, boutiques, produits) utilisent systématiquement l’entreprise de l’user et des repositories filtrés.
- **Rôles et permissions** (RoleType, PermissionType, CentralAccess) pour restreindre l’accès.
- **Sessions utilisateur** (UserSession) avec vérification optionnelle dans le token.

### Manques et risques

- **Pas de filtre JPA global** : tout `findById(id)` sans `entrepriseId` peut exposer des données d’un autre tenant ; de nombreux usages existent (voir section 8).
- **Path variable non utilisée** : `GET /entreprise/{entrepriseId}/allusers` retourne toujours les users de l’entreprise de l’user connecté ; aligner l’API (documentation ou utilisation réelle de `entrepriseId` avec vérification).
- **Pas de TenantContext** : pas de thread-local « tenant courant » ; chaque service refait authHelper + getEntreprise (risque d’oublis sur nouveaux endpoints).
- **FactureVente / Vente** : pas de colonne `entreprise_id` directe ; l’isolation repose sur les jointures (Vente → Boutique → Entreprise). Une colonne redondante `entreprise_id` pourrait simplifier requêtes et contrôles.
- **Scalabilité** : une seule base partagée ; pas de stratégie « database per tenant » ni « schema per tenant ». Pour une forte croissance, prévoir partitionnement ou read-replicas.
- **Audit / conformité** : pas de traçabilité systématique « qui a accédé à quelles données de quel tenant ».

### Verdict

**Le système est structurellement prêt pour un SaaS multi-tenant** (modèle Entreprise, JWT, repositories et services majoritairement scopés par entreprise), mais **il n’est pas encore prêt à supporter « beaucoup d’entreprises et leurs utilisateurs » en toute sécurité** tant que :

1. Les appels `findById` sur les entités scopées par entreprise ne sont pas audités et sécurisés (remplacer par `findByIdAndEntrepriseId` ou vérification explicite de l’entreprise).
2. Un filtre Hibernate `@Filter` (ou équivalent) n’est pas envisagé pour les entités liées à l’entreprise, afin d’éviter les oublis.
3. L’API et la documentation ne sont pas alignées (ex. path `entrepriseId` pour `/entreprise/{entrepriseId}/allusers`).

Après ces corrections, l’architecture actuelle peut supporter un SaaS multi-tenant avec entreprises, utilisateurs, boutiques, produits, factures (achat/vente), stock, ventes, trésorerie, comptabilité et dépenses, avec une base partagée. Pour des volumes très élevés, une stratégie de scalabilité (partitionnement, réplicas, cache) devra être ajoutée.

---

## 10. Recommandations prioritaires

1. **Auditer et corriger** tous les `findById` / `getOne` sur entités liées à une entreprise : utiliser `findByIdAndEntrepriseId` ou vérifier l’entreprise après chargement avant toute utilisation.
2. **Introduire un filtre Hibernate `@Filter`** sur les entités scopées par entreprise (ou un AOP/intercepteur qui impose le filtre quand le tenant est connu).
3. **Aligner l’API** : soit supprimer `entrepriseId` du path de `/entreprise/{entrepriseId}/allusers`, soit l’utiliser et vérifier qu’il correspond à l’entreprise de l’user.
4. **Documenter** la stratégie de facturation SaaS (abonnements par entreprise) et l’isolation des données.
5. **Envisager** une colonne redondante `entreprise_id` sur Vente et FactureVente pour simplifier requêtes et contrôles d’isolation.

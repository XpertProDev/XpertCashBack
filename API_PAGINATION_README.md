# API Pagination - Produits par Entreprise et par Stock

## Vue d'ensemble

Ces APIs ont été conçues pour être **scalables** et supporter des **données volumineuses** dans un environnement SaaS. Elles remplacent les anciennes méthodes qui récupéraient tous les produits en une seule fois, ce qui pouvait causer des problèmes de performance avec de grandes quantités de données.

## Endpoints disponibles

### 1. Produits par Entreprise
```
GET /api/auth/entreprise/{entrepriseId}/produits/paginated
```

### 2. Produits par Stock (Boutique)
```
GET /api/auth/boutique/{boutiqueId}/produits/paginated
```

## Paramètres

### Path Parameters
- `entrepriseId` (Long, requis) : ID de l'entreprise (pour l'API entreprise)
- `boutiqueId` (Long, requis) : ID de la boutique (pour l'API stock)

### Query Parameters
- `page` (int, optionnel) : Numéro de la page (défaut: 0)
- `size` (int, optionnel) : Taille de la page (défaut: 20, max: 100)

### Headers
- `Authorization: Bearer {token}` (requis) : Token JWT de l'utilisateur

## Exemples d'utilisation

### API Produits par Entreprise

#### Récupérer la première page avec 20 éléments (défaut)
```bash
curl -X GET "http://localhost:8080/api/auth/entreprise/123/produits/paginated" \
  -H "Authorization: Bearer your-jwt-token"
```

#### Récupérer la page 2 avec 50 éléments
```bash
curl -X GET "http://localhost:8080/api/auth/entreprise/123/produits/paginated?page=1&size=50" \
  -H "Authorization: Bearer your-jwt-token"
```

#### Récupérer la page 5 avec 100 éléments (maximum)
```bash
curl -X GET "http://localhost:8080/api/auth/entreprise/123/produits/paginated?page=4&size=100" \
  -H "Authorization: Bearer your-jwt-token"
```

### API Produits par Stock (Boutique)

#### Récupérer la première page avec 20 éléments (défaut)
```bash
curl -X GET "http://localhost:8080/api/auth/boutique/456/produits/paginated" \
  -H "Authorization: Bearer your-jwt-token"
```

#### Récupérer la page 2 avec 30 éléments
```bash
curl -X GET "http://localhost:8080/api/auth/boutique/456/produits/paginated?page=1&size=30" \
  -H "Authorization: Bearer your-jwt-token"
```

#### Récupérer la page 3 avec 100 éléments (maximum)
```bash
curl -X GET "http://localhost:8080/api/auth/boutique/456/produits/paginated?page=2&size=100" \
  -H "Authorization: Bearer your-jwt-token"
```

## Réponse

### API Produits par Entreprise

```json
{
  "content": [
    {
      "id": 1,
      "nom": "Produit A",
      "prixVente": 1000.0,
      "prixAchat": 500.0,
      "quantite": 150,
      "codeGenerique": "PROD001",
      "boutiques": [
        {
          "id": 1,
          "nom": "Boutique Centre",
          "typeBoutique": "PHYSIQUE",
          "quantite": 75
    },
    {
      "id": 2,
          "nom": "Boutique Nord",
          "typeBoutique": "PHYSIQUE",
          "quantite": 75
        }
      ]
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 150,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false,
  "isFirst": true,
  "isLast": false,
  "totalProduitsUniques": 150,
  "totalBoutiques": 5
}
```

### API Produits par Stock (Boutique)

```json
{
  "content": [
    {
      "id": 1,
      "nom": "Produit A",
      "prixVente": 1000.0,
      "prixAchat": 500.0,
      "quantite": 75,
      "categorieId": 2,
      "nomCategorie": "Électronique",
      "uniteId": 1,
      "nomUnite": "Pièce",
      "enStock": true,
      "boutiqueId": 456
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 85,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false,
  "isFirst": true,
  "isLast": false,
  "totalProduitsActifs": 85,
  "totalProduitsEnStock": 65,
  "totalProduitsHorsStock": 20
}
```

## Structure de la réponse

### API Produits par Entreprise

| Champ | Type | Description |
|-------|------|-------------|
| `content` | Array | Liste des produits de la page courante |
| `pageNumber` | int | Numéro de la page actuelle (0-based) |
| `pageSize` | int | Taille de la page |
| `totalElements` | long | Nombre total de produits uniques |
| `totalPages` | int | Nombre total de pages |
| `hasNext` | boolean | Y a-t-il une page suivante ? |
| `hasPrevious` | boolean | Y a-t-il une page précédente ? |
| `isFirst` | boolean | Est-ce la première page ? |
| `isLast` | boolean | Est-ce la dernière page ? |
| `totalProduitsUniques` | long | Nombre total de produits uniques par code générique |
| `totalBoutiques` | long | Nombre total de boutiques actives |

### API Produits par Stock (Boutique)

| Champ | Type | Description |
|-------|------|-------------|
| `content` | Array | Liste des produits de la page courante |
| `pageNumber` | int | Numéro de la page actuelle (0-based) |
| `pageSize` | int | Taille de la page |
| `totalElements` | long | Nombre total de produits actifs |
| `totalPages` | int | Nombre total de pages |
| `hasNext` | boolean | Y a-t-il une page suivante ? |
| `hasPrevious` | boolean | Y a-t-il une page précédente ? |
| `isFirst` | boolean | Est-ce la première page ? |
| `isLast` | boolean | Est-ce la dernière page ? |
| `totalProduitsActifs` | long | Nombre total de produits actifs |
| `totalProduitsEnStock` | long | Nombre total de produits en stock |
| `totalProduitsHorsStock` | long | Nombre total de produits hors stock |

## Avantages de la pagination

### 1. **Performance**
- Chargement plus rapide des pages
- Moins de mémoire utilisée côté serveur
- Réponse plus rapide pour l'utilisateur

### 2. **Scalabilité**
- Support de milliers/millions de produits
- Performance constante quelle que soit la taille des données
- Évite les timeouts sur de gros volumes

### 3. **UX améliorée**
- Interface plus réactive
- Possibilité de navigation par pages
- Affichage progressif des données

### 4. **Sécurité**
- Limitation de la taille des requêtes
- Protection contre les attaques DoS
- Contrôle des ressources serveur

## Implémentation technique

### API Produits par Entreprise

#### Repository
```java
@Query("SELECT p FROM Produit p " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE (b.entreprise.id = :entrepriseId OR b IS NULL) " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "ORDER BY p.codeGenerique, p.nom")
Page<Produit> findProduitsByEntrepriseIdPaginated(
    @Param("entrepriseId") Long entrepriseId, 
    Pageable pageable);
```

#### Service
- Validation des paramètres de pagination
- Gestion des droits d'accès
- Optimisation des requêtes avec JOIN FETCH
- Groupement par code générique
- Calcul des statistiques globales

### API Produits par Stock (Boutique)

#### Repository
```java
@Query("SELECT p FROM Produit p " +
       "LEFT JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.uniteDeMesure u " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE p.boutique.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "ORDER BY p.nom ASC")
Page<Produit> findProduitsByBoutiqueIdPaginated(
    @Param("boutiqueId") Long boutiqueId, 
    Pageable pageable);
```

#### Service
- Validation des paramètres de pagination
- Gestion des droits d'accès et vérification d'affectation boutique
- Optimisation des requêtes avec JOIN FETCH
- Calcul des statistiques de stock (en stock, hors stock)

### Contrôleur
- Validation des paramètres
- Gestion des erreurs
- Limitation de la taille maximale (100)

## Bonnes pratiques

### 1. **Taille de page recommandée**
- **Mobile** : 10-20 éléments
- **Desktop** : 20-50 éléments
- **Tableau de bord** : 50-100 éléments

### 2. **Navigation**
- Toujours afficher le numéro de page actuel
- Indiquer le nombre total de pages
- Fournir des boutons précédent/suivant
- Permettre de sauter à une page spécifique

### 3. **Gestion des erreurs**
- Valider les paramètres côté client et serveur
- Gérer les cas de page invalide
- Retourner des messages d'erreur clairs

### 4. **Cache**
- Mettre en cache les statistiques globales
- Utiliser le cache pour les données fréquemment consultées
- Invalider le cache lors des modifications

## Migration depuis l'ancienne API

### API Produits par Entreprise

#### Avant (non-scalable)
```java
// Récupérait TOUS les produits
List<ProduitDTO> produits = produitService.getProduitsParEntreprise(entrepriseId, request);
```

#### Après (scalable)
```java
// Récupère seulement une page
ProduitEntreprisePaginatedResponseDTO response = produitService.getProduitsParEntreprisePaginated(
    entrepriseId, page, size, request);

List<ProduitDTO> produits = response.getContent();
int totalPages = response.getTotalPages();
boolean hasNext = response.hasNext();
```

### API Produits par Stock (Boutique)

#### Avant (non-scalable)
```java
// Récupérait TOUS les produits
List<ProduitDTO> produits = produitService.getProduitsParStock(boutiqueId, request);
```

#### Après (scalable)
```java
// Récupère seulement une page
ProduitStockPaginatedResponseDTO response = produitService.getProduitsParStockPaginated(
    boutiqueId, page, size, request);

List<ProduitDTO> produits = response.getContent();
int totalPages = response.getTotalPages();
boolean hasNext = response.hasNext();
long totalEnStock = response.getTotalProduitsEnStock();
long totalHorsStock = response.getTotalProduitsHorsStock();
```

## Monitoring et métriques

### Métriques à surveiller
- Temps de réponse par page
- Taille des réponses
- Utilisation mémoire
- Nombre de requêtes par seconde

### Alertes
- Temps de réponse > 2 secondes
- Utilisation mémoire > 80%
- Erreur 500 > 5%

## Conclusion

Cette implémentation de pagination transforme vos APIs en des solutions **entreprise-grade** capables de gérer des volumes de données importants tout en maintenant des performances optimales. 

### 🚀 **APIs disponibles**

1. **Produits par Entreprise** : Gestion centralisée de tous les produits d'une entreprise avec groupement par code générique
2. **Produits par Stock (Boutique)** : Gestion locale des produits d'une boutique spécifique avec statistiques de stock

### 💡 **Avantages clés**

- **Scalabilité** : Support de milliers/millions de produits
- **Performance** : Temps de réponse constant quelle que soit la taille des données
- **UX** : Interface réactive avec navigation par pages
- **Sécurité** : Contrôle des droits d'accès et limitation des ressources
- **Monitoring** : Statistiques détaillées pour le suivi des performances

### 🎯 **Cas d'usage recommandés**

- **API Entreprise** : Tableaux de bord, rapports globaux, gestion centralisée
- **API Stock** : Gestion de boutique, inventaire local, vente au détail

Ces APIs respectent les meilleures pratiques de développement SaaS et offrent une expérience utilisateur fluide même avec des volumes de données importants.

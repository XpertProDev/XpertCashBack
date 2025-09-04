# Script d'ajout de produits avec images

Ce dossier contient des scripts pour ajouter automatiquement 40 produits dans votre boutique XpertCash avec des catégories variées et des images.

## 📁 Fichiers inclus

- `add_40_products_with_images.sh` - Script principal pour ajouter les produits
- `config_products.sh` - Fichier de configuration
- `get_api_info.sh` - Script d'aide pour obtenir les informations API
- `README_PRODUCTS_SCRIPT.md` - Ce fichier d'instructions

## 🚀 Utilisation rapide

### 1. Obtenir les informations nécessaires
```bash
./get_api_info.sh
```
Ce script vous aidera à obtenir :
- Votre token JWT
- Les IDs des boutiques disponibles
- Les IDs des catégories existantes
- Les IDs des unités disponibles

### 2. Configurer le script
Modifiez le fichier `config_products.sh` avec vos vraies valeurs :
```bash
nano config_products.sh
```

### 3. Exécuter le script
```bash
source config_products.sh
./add_40_products_with_images.sh
```

## 📦 Produits inclus

Le script ajoute **40 produits** répartis comme suit :

### Avec catégories (35 produits) :
- **Électronique** (5 produits) : iPhone, Samsung, MacBook, AirPods, iPad
- **Vêtements** (5 produits) : T-shirt Nike, Jean Levis, Veste Adidas, etc.
- **Alimentation** (5 produits) : Pommes, Pain, Lait, Fromage, Chocolat
- **Maison & Jardin** (5 produits) : Aspirateur, Cafetière, Coussin, Plante, Bougie
- **Sports & Loisirs** (5 produits) : Vélo, Raquette, Ballon, Tapis yoga, Haltères
- **Livres & Médias** (5 produits) : Harry Potter, Romans, BD, Dictionnaire
- **Beauté & Santé** (5 produits) : Crème, Parfum, Rouge à lèvres, Shampoing
- **Automobile** (5 produits) : Pneus, Huile, Batterie, Pare-brise, Feux
- **Bricolage** (5 produits) : Perceuse, Marteau, Vis, Peinture, Pinceaux
- **Jouets & Jeux** (5 produits) : Lego, Barbie, Monopoly, Puzzle, Voiture RC

### Sans catégorie (5 produits) :
- 5 "Produits mystère" qui seront automatiquement placés dans la catégorie "Sans Category"

## 🖼️ Images

Tous les produits incluent des images téléchargées depuis Unsplash :
- Images haute qualité (400px de largeur)
- Téléchargement automatique et temporaire
- Nettoyage automatique après utilisation

## ⚙️ Configuration requise

### Prérequis
- `curl` installé
- `jq` installé (optionnel, pour un meilleur affichage JSON)
- Votre application XpertCash en cours d'exécution
- Token JWT valide

### Variables à configurer
Dans `config_products.sh` :
- `BASE_URL` : URL de votre API (par défaut: http://localhost:8080/api/auth)
- `TOKEN` : Votre token JWT
- `BOUTIQUE_ID` : ID de votre boutique
- IDs des catégories et unités

## 🔧 Dépannage

### Erreur "Token JWT manquant"
- Vérifiez que votre token JWT est correct
- Assurez-vous que votre session n'a pas expiré

### Erreur "Boutique introuvable"
- Vérifiez l'ID de votre boutique
- Utilisez `./get_api_info.sh` pour obtenir les bons IDs

### Erreur "Catégorie non trouvée"
- Le script crée automatiquement les catégories manquantes
- Vérifiez que vous avez les permissions nécessaires

### Images non téléchargées
- Vérifiez votre connexion internet
- Certaines images Unsplash peuvent être temporairement indisponibles

## 📊 Résultats attendus

Après exécution réussie :
- ✅ 40 produits créés
- ✅ 10 catégories créées (si elles n'existent pas)
- ✅ 5 produits dans "Sans Category"
- ✅ Tous les produits ajoutés au stock
- ✅ Images téléchargées et associées

## 🎯 Personnalisation

Vous pouvez facilement modifier le script pour :
- Ajouter plus de produits
- Changer les catégories
- Modifier les prix et quantités
- Utiliser d'autres sources d'images
- Ajouter des dates de péremption

## 📞 Support

En cas de problème :
1. Vérifiez les logs du script
2. Vérifiez que votre API fonctionne
3. Vérifiez vos permissions utilisateur
4. Consultez les logs de votre application XpertCash

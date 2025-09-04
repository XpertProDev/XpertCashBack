#!/bin/bash

# Configuration pour le script d'ajout de produits
# Modifiez ces valeurs selon votre environnement

# URL de base de votre API
export BASE_URL="http://localhost:8080/api/auth"

# Votre token JWT (obtenez-le en vous connectant)
export TOKEN="YOUR_JWT_TOKEN_HERE"

# ID de votre boutique (obtenez-le via l'API /api/auth/boutiqueEntreprise)
export BOUTIQUE_ID="1"

# IDs des catégories (ajustez selon votre base de données)
# Vous pouvez les obtenir via l'API /api/auth/allCategories
export CAT_ELECTRONIQUE=1
export CAT_VETEMENTS=2
export CAT_ALIMENTATION=3
export CAT_MAISON=4
export CAT_SPORTS=5
export CAT_LIVRES=6
export CAT_BEAUTE=7
export CAT_AUTO=8
export CAT_BRICOLAGE=9
export CAT_JOUETS=10
export CAT_SANS_CATEGORIE=null

# IDs des unités (ajustez selon votre base de données)
# Vous pouvez les obtenir via l'API /api/auth/allUnite
export UNITE_PIECE=1
export UNITE_KG=2
export UNITE_LITRE=3
export UNITE_METRE=4

echo "Configuration chargée !"
echo "BASE_URL: $BASE_URL"
echo "BOUTIQUE_ID: $BOUTIQUE_ID"
echo ""
echo "Pour utiliser le script:"
echo "1. Modifiez ce fichier avec vos vraies valeurs"
echo "2. Exécutez: source config_products.sh"
echo "3. Exécutez: ./add_40_products_with_images.sh"

#!/bin/bash

# Script pour obtenir les informations nécessaires pour configurer l'ajout de produits

BASE_URL="http://localhost:8080/api/auth"

echo "🔍 Script d'aide pour obtenir les informations API"
echo "=================================================="
echo ""

# Fonction pour faire une requête avec token
make_request() {
    local endpoint="$1"
    local token="$2"
    
    if [ -z "$token" ] || [ "$token" = "YOUR_JWT_TOKEN_HERE" ]; then
        echo "❌ Token JWT requis. Connectez-vous d'abord pour obtenir votre token."
        return 1
    fi
    
    curl -s -H "Authorization: Bearer $token" "$BASE_URL$endpoint" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $token" "$BASE_URL$endpoint"
}

echo "📋 Instructions:"
echo "1. Connectez-vous à votre application pour obtenir votre token JWT"
echo "2. Entrez votre token JWT ci-dessous"
echo "3. Le script récupérera automatiquement les IDs des catégories et unités"
echo ""

read -p "🔑 Entrez votre token JWT: " JWT_TOKEN

if [ -z "$JWT_TOKEN" ] || [ "$JWT_TOKEN" = "YOUR_JWT_TOKEN_HERE" ]; then
    echo "❌ Token JWT invalide. Arrêt du script."
    exit 1
fi

echo ""
echo "🔍 Récupération des informations..."

# Récupérer les boutiques
echo ""
echo "🏪 BOUTIQUES DISPONIBLES:"
echo "========================="
BOUTIQUES=$(make_request "/boutiqueEntreprise" "$JWT_TOKEN")
echo "$BOUTIQUES"

# Récupérer les catégories
echo ""
echo "📂 CATÉGORIES DISPONIBLES:"
echo "=========================="
CATEGORIES=$(make_request "/allCategories" "$JWT_TOKEN")
echo "$CATEGORIES"

# Récupérer les unités
echo ""
echo "📏 UNITÉS DISPONIBLES:"
echo "======================"
UNITES=$(make_request "/allUnite" "$JWT_TOKEN")
echo "$UNITES"

echo ""
echo "✅ Informations récupérées !"
echo ""
echo "📝 Pour configurer le script d'ajout de produits:"
echo "1. Copiez les IDs des boutiques, catégories et unités ci-dessus"
echo "2. Modifiez le fichier config_products.sh avec ces valeurs"
echo "3. Exécutez: source config_products.sh && ./add_40_products_with_images.sh"
echo ""
echo "💡 Conseil: Sauvegardez ces informations dans un fichier pour référence future."

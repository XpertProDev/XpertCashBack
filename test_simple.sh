#!/bin/bash

# Script de test simple pour l'API de pagination
# Remplacez TOKEN_VALIDE par votre vrai token JWT

BASE_URL="http://localhost:8080"
ENTREPRISE_ID="1"
TOKEN_VALIDE="VOTRE_TOKEN_JWT_ICI"  # ⚠️ Remplacez par votre vrai token !

echo "🧪 Test simple de l'API de pagination"
echo "====================================="

if [ "$TOKEN_VALIDE" = "VOTRE_TOKEN_JWT_ICI" ]; then
    echo "❌ ERREUR: Vous devez remplacer TOKEN_VALIDE par votre vrai token JWT"
    echo ""
    echo "📝 Comment obtenir un token valide :"
    echo "   1. Connectez-vous à votre application"
    echo "   2. Récupérez le token JWT depuis les cookies ou le localStorage"
    echo "   3. Remplacez TOKEN_VALIDE dans ce script"
    echo ""
    echo "🔧 Ou utilisez le script de débogage :"
    echo "   ./debug_api.sh"
    exit 1
fi

echo "✅ Token configuré, test en cours..."
echo ""

echo "📄 Test 1: Première page (taille par défaut)"
response=$(curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated" \
  -H "Authorization: Bearer $TOKEN_VALIDE" \
  -H "Content-Type: application/json" \
  -w "HTTP_STATUS:%{http_code}")

# Extraire le status HTTP et la réponse
http_status=$(echo "$response" | grep -o 'HTTP_STATUS:[0-9]*' | cut -d: -f2)
api_response=$(echo "$response" | sed 's/HTTP_STATUS:[0-9]*//')

echo "   Status HTTP: $http_status"
echo "   Réponse:"
echo "$api_response" | jq '.' 2>/dev/null || echo "$api_response"

echo ""

if [ "$http_status" = "200" ]; then
    echo "✅ Succès ! L'API fonctionne correctement."
    echo ""
    echo "📊 Test de pagination avec paramètres..."
    
    echo "📄 Test 2: Page 0, taille 5"
    curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated?page=0&size=5" \
      -H "Authorization: Bearer $TOKEN_VALIDE" \
      -H "Content-Type: application/json" | jq '.'
      
else
    echo "❌ Échec ! Status HTTP: $http_status"
    echo ""
    echo "🔍 Vérifiez :"
    echo "   - Que votre token JWT est valide et non expiré"
    echo "   - Que l'utilisateur a accès à l'entreprise $ENTREPRISE_ID"
    echo "   - Que l'application est bien démarrée"
    echo ""
    echo "📝 Utilisez le script de débogage pour plus d'informations :"
    echo "   ./debug_api.sh"
fi




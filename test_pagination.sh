#!/bin/bash

# Script de test pour l'API de pagination des produits par entreprise
# Assurez-vous que votre application Spring Boot est en cours d'exécution

BASE_URL="http://localhost:8080"
TOKEN="your-jwt-token-here"  # Remplacez par votre vrai token JWT
ENTREPRISE_ID="1"  # Remplacez par l'ID de votre entreprise

echo "🧪 Test de l'API de pagination des produits par entreprise"
echo "=================================================="

# Test 1: Première page avec taille par défaut (20)
echo "📄 Test 1: Première page (taille par défaut)"
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 2: Deuxième page avec 10 éléments
echo "📄 Test 2: Deuxième page avec 10 éléments"
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 3: Page avec taille maximale (100)
echo "📄 Test 3: Page avec taille maximale (100)"
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated?page=0&size=100" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 4: Page invalide (négative)
echo "📄 Test 4: Page invalide (négative)"
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated?page=-1&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 5: Taille invalide (trop grande)
echo "📄 Test 5: Taille invalide (trop grande)"
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated?page=0&size=150" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 6: Sans token d'autorisation
echo "📄 Test 6: Sans token d'autorisation"
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

echo "✅ Tests terminés !"
echo "📊 Vérifiez les réponses ci-dessus pour vous assurer que la pagination fonctionne correctement."
echo "🔍 Points à vérifier :"
echo "   - Les métadonnées de pagination sont présentes"
echo "   - Le contenu est limité à la taille de page demandée"
echo "   - Les statistiques globales sont correctes"
echo "   - La gestion des erreurs fonctionne"




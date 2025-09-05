#!/bin/bash

# Test script pour la pagination des factures réelles
# Assurez-vous d'avoir un token JWT valide

# Configuration
BASE_URL="http://localhost:8080/api/auth"
# Remplacez par votre token JWT valide
JWT_TOKEN="YOUR_JWT_TOKEN_HERE"

echo "🧪 Test de la pagination des factures réelles"
echo "=============================================="

# Test 1: Première page avec 10 éléments
echo "📄 Test 1: Première page (page=0, size=10)"
curl -X GET "$BASE_URL/mes-factures-reelles/paginated?page=0&size=10" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n"

# Test 2: Deuxième page avec 10 éléments
echo "📄 Test 2: Deuxième page (page=1, size=10)"
curl -X GET "$BASE_URL/mes-factures-reelles/paginated?page=1&size=10" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n"

# Test 3: Page avec 5 éléments seulement
echo "📄 Test 3: Petite page (page=0, size=5)"
curl -X GET "$BASE_URL/mes-factures-reelles/paginated?page=0&size=5" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n"

# Test 4: Paramètres par défaut
echo "📄 Test 4: Paramètres par défaut (sans page ni size)"
curl -X GET "$BASE_URL/mes-factures-reelles/paginated" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n"

# Test 5: Comparaison avec l'ancienne API
echo "📄 Test 5: Ancienne API (sans pagination)"
curl -X GET "$BASE_URL/mes-factures-reelles" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  | jq '. | length'

echo -e "\n🎉 Tests terminés!"

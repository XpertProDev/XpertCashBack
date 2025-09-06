#!/bin/bash

echo "🧪 TEST DU CACHE DES FACTURES RÉELLES"
echo "====================================="

BASE_URL="http://localhost:8080/api/auth"

echo "📊 Test 1: Vérification des endpoints de cache factures réelles"
echo "--------------------------------------------------------------"

echo "✅ Test vidage cache factures-reelles:"
curl -s -X POST "$BASE_URL/cache/evict/factures-reelles" | jq .

echo ""
echo "📊 Test 2: Test de performance des factures réelles paginées"
echo "-----------------------------------------------------------"

echo "⏱️  Premier appel factures réelles paginées (sans cache):"
time curl -s -X GET "$BASE_URL/mes-factures-reelles/paginated?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Deuxième appel factures réelles paginées (avec cache):"
time curl -s -X GET "$BASE_URL/mes-factures-reelles/paginated?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Premier appel filtrage par mois/année (sans cache):"
time curl -s -X GET "$BASE_URL/filtrer-facturesReelles?mois=12&annee=2024" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Deuxième appel filtrage par mois/année (avec cache):"
time curl -s -X GET "$BASE_URL/filtrer-facturesReelles?mois=12&annee=2024" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "📊 Test 3: Vérification de l'invalidation automatique"
echo "----------------------------------------------------"

echo "🔄 Test après vidage du cache..."
curl -s -X POST "$BASE_URL/cache/evict/factures-reelles" | jq .

echo ""
echo "⏱️  Appel après vidage (devrait être plus lent):"
time curl -s -X GET "$BASE_URL/mes-factures-reelles/paginated?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "📊 Test 4: Test avec différentes pages"
echo "-------------------------------------"

echo "⏱️  Page 0, Size 10:"
time curl -s -X GET "$BASE_URL/mes-factures-reelles/paginated?page=0&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Page 1, Size 20:"
time curl -s -X GET "$BASE_URL/mes-factures-reelles/paginated?page=1&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "✅ Tests de cache des factures réelles terminés!"
echo ""
echo "💡 Pour tester avec des données réelles:"
echo "1. Assurez-vous d'avoir un token JWT valide"
echo "2. Remplacez YOUR_TOKEN_HERE par votre token"
echo "3. Exécutez: ./test_cache_factures_reelles.sh"
echo ""
echo "🎯 Endpoints mis en cache:"
echo "   - GET /mes-factures-reelles/paginated (cache: factures-reelles)"
echo "   - GET /filtrer-facturesReelles (cache: factures-reelles)"
echo ""
echo "🔄 Invalidation automatique sur:"
echo "   - annulerFactureReelle() → invalide factures-reelles"

#!/bin/bash

echo "🧪 TEST DU CACHE DES STATISTIQUES DE VENTES"
echo "==========================================="

BASE_URL="http://localhost:8080/api/auth"

echo "📊 Test 1: Vérification des endpoints de cache ventes"
echo "----------------------------------------------------"

echo "✅ Test vidage cache ventes-stats:"
curl -s -X POST "$BASE_URL/cache/evict/ventes-stats" | jq .

echo ""
echo "📊 Test 2: Test de performance des statistiques"
echo "-----------------------------------------------"

echo "⏱️  Premier appel montant total jour (sans cache):"
time curl -s -X GET "$BASE_URL/vente/montant-total-jour" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Deuxième appel montant total jour (avec cache):"
time curl -s -X GET "$BASE_URL/vente/montant-total-jour" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Premier appel montant total mois (sans cache):"
time curl -s -X GET "$BASE_URL/vente/montant-total-mois" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Deuxième appel montant total mois (avec cache):"
time curl -s -X GET "$BASE_URL/vente/montant-total-mois" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Premier appel bénéfice net (sans cache):"
time curl -s -X GET "$BASE_URL/vente/benefice-net" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Deuxième appel bénéfice net (avec cache):"
time curl -s -X GET "$BASE_URL/vente/benefice-net" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "📊 Test 3: Vérification de l'invalidation automatique"
echo "----------------------------------------------------"

echo "🔄 Test après vidage du cache..."
curl -s -X POST "$BASE_URL/cache/evict/ventes-stats" | jq .

echo ""
echo "⏱️  Appel après vidage (devrait être plus lent):"
time curl -s -X GET "$BASE_URL/vente/montant-total-jour" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "✅ Tests de cache des statistiques de ventes terminés!"
echo ""
echo "💡 Pour tester avec des données réelles:"
echo "1. Assurez-vous d'avoir un token JWT valide"
echo "2. Remplacez YOUR_TOKEN_HERE par votre token"
echo "3. Exécutez: ./test_cache_ventes_stats.sh"

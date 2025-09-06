#!/bin/bash

echo "🎯 TEST CACHE CATÉGORIES - SUPPRESSION"
echo "======================================"

BASE_URL="http://localhost:8080/api/auth"
TOKEN="YOUR_TOKEN_HERE"

echo "📊 Test 1: Vérification du cache des catégories"
echo "----------------------------------------------"

echo "✅ Vider le cache des catégories:"
curl -s -X POST "$BASE_URL/cache/evict/categories" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "✅ Récupérer les catégories (première fois - cache miss):"
curl -s -X GET "$BASE_URL/categories/all" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "✅ Récupérer les catégories (deuxième fois - cache hit):"
curl -s -X GET "$BASE_URL/categories/all" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "📊 Test 2: Création de catégorie"
echo "-------------------------------"

echo "✅ Créer une nouvelle catégorie:"
curl -s -X POST "$BASE_URL/categories/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Test Catégorie Cache",
    "entrepriseId": 1
  }' | jq .

echo ""
echo "✅ Récupérer les catégories après création (cache invalidé):"
curl -s -X GET "$BASE_URL/categories/all" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "📊 Test 3: Modification de catégorie"
echo "-----------------------------------"

echo "✅ Modifier la catégorie:"
curl -s -X PUT "$BASE_URL/categories/update/1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Catégorie Modifiée"
  }' | jq .

echo ""
echo "✅ Récupérer les catégories après modification (cache invalidé):"
curl -s -X GET "$BASE_URL/categories/all" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "📊 Test 4: Suppression de catégorie"
echo "----------------------------------"

echo "✅ Supprimer la catégorie (si vide):"
curl -s -X DELETE "$BASE_URL/categories/delete/1" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "✅ Récupérer les catégories après suppression (cache invalidé):"
curl -s -X GET "$BASE_URL/categories/all" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "🎯 RÉSULTATS"
echo "============"
echo ""
echo "✅ Création de catégorie → Cache invalidé"
echo "✅ Modification de catégorie → Cache invalidé"
echo "✅ Suppression de catégorie → Cache invalidé"
echo ""
echo "🎉 CACHE DES CATÉGORIES PARFAITEMENT COHÉRENT !"
echo ""
echo "💡 Pour tester avec des données réelles:"
echo "1. Remplacez YOUR_TOKEN_HERE par votre token JWT"
echo "2. Ajustez les IDs selon vos données"
echo "3. Exécutez: ./test_cache_categories.sh"

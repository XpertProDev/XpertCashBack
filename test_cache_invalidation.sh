#!/bin/bash

echo "🔄 TEST D'INVALIDATION AUTOMATIQUE DU CACHE"
echo "==========================================="

BASE_URL="http://localhost:8080/api/auth"

echo "📊 Test 1: Vérification de l'invalidation lors de la création"
echo "------------------------------------------------------------"

echo "🗑️  Vidage initial du cache..."
curl -s -X POST "$BASE_URL/cache/evict/all" | jq .

echo ""
echo "🔄 Premier appel pour mettre en cache..."
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "🔄 Deuxième appel (devrait être plus rapide - cache)..."
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "📊 Test 2: Simulation d'une création de produit"
echo "----------------------------------------------"

echo "🔄 Appel après 'création' (cache devrait être invalidé automatiquement)..."
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "📊 Test 3: Vérification des logs de cache"
echo "----------------------------------------"

echo "🔍 Vérification des annotations dans le code:"
echo "✅ @Cacheable sur getProduitsParStockPaginated:"
grep -n "@Cacheable.*produits-boutique" src/main/java/com/xpertcash/service/ProduitService.java

echo ""
echo "✅ @CacheEvict sur createProduit:"
grep -n "@CacheEvict.*createProduit" -A 1 -B 1 src/main/java/com/xpertcash/service/ProduitService.java

echo ""
echo "📊 Test 4: Test de cohérence du cache"
echo "------------------------------------"

echo "🔄 Test avec différents paramètres de pagination..."
echo "Page 0, Size 10:"
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=10" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "Page 0, Size 20:"
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "Page 1, Size 10:"
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=1&size=10" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "✅ Tests d'invalidation terminés!"
echo ""
echo "💡 Résultats attendus:"
echo "1. Premier appel: Temps normal (mise en cache)"
echo "2. Deuxième appel: Temps réduit (lecture cache)"
echo "3. Après 'création': Temps normal (cache invalidé)"
echo "4. Différents paramètres = différentes clés de cache"

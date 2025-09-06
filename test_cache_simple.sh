#!/bin/bash

echo "🧪 TEST SIMPLE DU CACHE"
echo "======================="

BASE_URL="http://localhost:8080/api/auth"

echo "📊 Test 1: Vérification des endpoints de cache"
echo "---------------------------------------------"

echo "✅ Test vidage cache produits-boutique:"
curl -s -X POST "$BASE_URL/cache/evict/produits-boutique" | jq .

echo ""
echo "✅ Test vidage cache produits-entreprise:"
curl -s -X POST "$BASE_URL/cache/evict/produits-entreprise" | jq .

echo ""
echo "✅ Test vidage cache stock-historique:"
curl -s -X POST "$BASE_URL/cache/evict/stock-historique" | jq .

echo ""
echo "✅ Test vidage tous les caches:"
curl -s -X POST "$BASE_URL/cache/evict/all" | jq .

echo ""
echo "📊 Test 2: Vérification de la configuration du cache"
echo "---------------------------------------------------"

echo "🔍 Vérification de la configuration dans application-dev.properties:"
grep -n "spring.cache" src/main/resources/application-dev.properties

echo ""
echo "🔍 Vérification de la classe CacheConfig:"
grep -n "CacheManager\|@EnableCaching" src/main/java/com/xpertcash/configuration/CacheConfig.java

echo ""
echo "🔍 Vérification des annotations @Cacheable dans ProduitService:"
grep -n "@Cacheable" src/main/java/com/xpertcash/service/ProduitService.java

echo ""
echo "🔍 Vérification des annotations @CacheEvict dans ProduitService:"
grep -n "@CacheEvict" src/main/java/com/xpertcash/service/ProduitService.java

echo ""
echo "✅ Tests de base terminés!"
echo ""
echo "💡 Pour tester avec des données réelles:"
echo "1. Assurez-vous d'avoir un token JWT valide"
echo "2. Remplacez YOUR_TOKEN_HERE dans test_cache_performance.sh"
echo "3. Utilisez des IDs de boutique/entreprise/produit valides"
echo "4. Exécutez: ./test_cache_performance.sh"

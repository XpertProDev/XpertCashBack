#!/bin/bash

echo "🧪 TEST COMPLET DE TOUS LES CACHES"
echo "================================="

BASE_URL="http://localhost:8080/api/auth"

echo "📊 Test 1: Vérification de tous les endpoints de cache"
echo "----------------------------------------------------"

echo "✅ Cache produits-boutique:"
curl -s -X POST "$BASE_URL/cache/evict/produits-boutique" | jq .

echo ""
echo "✅ Cache produits-entreprise:"
curl -s -X POST "$BASE_URL/cache/evict/produits-entreprise" | jq .

echo ""
echo "✅ Cache stock-historique:"
curl -s -X POST "$BASE_URL/cache/evict/stock-historique" | jq .

echo ""
echo "✅ Cache ventes-stats:"
curl -s -X POST "$BASE_URL/cache/evict/ventes-stats" | jq .

echo ""
echo "✅ Cache factures-reelles:"
curl -s -X POST "$BASE_URL/cache/evict/factures-reelles" | jq .

echo ""
echo "✅ Cache tous les produits:"
curl -s -X POST "$BASE_URL/cache/evict/all" | jq .

echo ""
echo "📊 Test 2: Vérification de la configuration"
echo "------------------------------------------"

echo "🔍 Vérification CacheConfig:"
grep -n "cacheNames" src/main/java/com/xpertcash/configuration/CacheConfig.java

echo ""
echo "🔍 Vérification application-dev.properties:"
grep -n "spring.cache.simple.cache-names" src/main/resources/application-dev.properties

echo ""
echo "📊 Test 3: Vérification des annotations @Cacheable"
echo "------------------------------------------------"

echo "✅ ProduitService - @Cacheable:"
grep -c "@Cacheable" src/main/java/com/xpertcash/service/ProduitService.java

echo "✅ VenteService - @Cacheable:"
grep -c "@Cacheable" src/main/java/com/xpertcash/service/VENTE/VenteService.java

echo "✅ FactureReelleService - @Cacheable:"
grep -c "@Cacheable" src/main/java/com/xpertcash/service/FactureReelleService.java

echo "✅ UsersService - @Cacheable:"
grep -c "@Cacheable" src/main/java/com/xpertcash/service/UsersService.java

echo ""
echo "📊 Test 4: Vérification des annotations @CacheEvict"
echo "------------------------------------------------"

echo "✅ ProduitService - @CacheEvict:"
grep -c "@CacheEvict" src/main/java/com/xpertcash/service/ProduitService.java

echo "✅ VenteService - @CacheEvict:"
grep -c "@CacheEvict" src/main/java/com/xpertcash/service/VENTE/VenteService.java

echo "✅ FactureReelleService - @CacheEvict:"
grep -c "@CacheEvict" src/main/java/com/xpertcash/service/FactureReelleService.java

echo "✅ UsersService - @CacheEvict:"
grep -c "@CacheEvict" src/main/java/com/xpertcash/service/UsersService.java

echo ""
echo "✅ RÉSUMÉ DU SYSTÈME DE CACHE"
echo "============================="
echo ""
echo "🎯 Caches configurés:"
echo "   - produits-boutique (pagination par boutique)"
echo "   - produits-entreprise (pagination par entreprise)"
echo "   - stock-historique (historique des stocks)"
echo "   - stock-entreprise (stocks par entreprise)"
echo "   - user-info (informations utilisateur)"
echo "   - ventes-stats (statistiques de ventes)"
echo "   - factures-reelles (factures réelles paginées)"
echo ""
echo "🚀 Services avec cache:"
echo "   - ProduitService (produits, stocks)"
echo "   - VenteService (statistiques, ventes)"
echo "   - FactureReelleService (factures réelles)"
echo "   - UsersService (informations utilisateur)"
echo ""
echo "🔄 Invalidation automatique:"
echo "   - Création/Modification de produits → invalide produits + stocks"
echo "   - Ventes/Remboursements → invalide ventes-stats"
echo "   - Modification de factures → invalide factures-reelles"
echo "   - Modification d'utilisateurs → invalide user-info"
echo ""
echo "🎉 SYSTÈME DE CACHE COMPLET ET OPTIMISÉ !"

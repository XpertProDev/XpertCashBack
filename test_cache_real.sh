#!/bin/bash

echo "🧪 TEST DU CACHE AVEC VOS VRAIES DONNÉES"
echo "========================================"

BASE_URL="http://localhost:8080/api/auth"

echo "📊 Test avec boutique ID 1 (comme dans vos logs)"
echo "-----------------------------------------------"

echo "🗑️  Étape 1: Vider le cache d'abord..."
curl -s -X POST "$BASE_URL/cache/evict/produits-boutique" | jq .

echo ""
echo "🔄 Étape 2: Premier appel (devrait exécuter la méthode et mettre en cache)"
echo "Regardez les logs de votre application - vous devriez voir:"
echo "🔍 CACHE DEBUG: Exécution de getProduitsParStockPaginated - Boutique: 1, Page: 0, Size: 20"
echo ""
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" | jq '.content | length' 2>/dev/null || echo "Requête effectuée"

echo ""
echo "🔄 Étape 3: Deuxième appel (devrait utiliser le cache)"
echo "Regardez les logs - vous NE devriez PAS voir le message CACHE DEBUG"
echo ""
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" | jq '.content | length' 2>/dev/null || echo "Requête effectuée"

echo ""
echo "🔄 Étape 4: Troisième appel (devrait utiliser le cache)"
echo "Regardez les logs - vous NE devriez PAS voir le message CACHE DEBUG"
echo ""
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" | jq '.content | length' 2>/dev/null || echo "Requête effectuée"

echo ""
echo "🔄 Étape 5: Appel avec paramètres différents (devrait exécuter la méthode)"
echo "Regardez les logs - vous devriez voir:"
echo "🔍 CACHE DEBUG: Exécution de getProduitsParStockPaginated - Boutique: 1, Page: 0, Size: 10"
echo ""
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=10" \
  -H "Content-Type: application/json" | jq '.content | length' 2>/dev/null || echo "Requête effectuée"

echo ""
echo "✅ Test terminé!"
echo ""
echo "💡 Interprétation des résultats:"
echo "1. Premier appel: Message CACHE DEBUG visible = Méthode exécutée (pas de cache)"
echo "2. Appels 2-3: Pas de message CACHE DEBUG = Cache utilisé"
echo "3. Appel avec size=10: Message CACHE DEBUG visible = Nouvelle clé de cache"
echo ""
echo "🔍 Si vous voyez le message CACHE DEBUG à chaque appel, le cache ne fonctionne pas"
echo "🔍 Si vous ne voyez le message qu'au premier appel, le cache fonctionne parfaitement!"


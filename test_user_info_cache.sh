#!/bin/bash

echo "🧪 TEST DU CACHE USER-INFO"
echo "=========================="

BASE_URL="http://localhost:8080/api/auth"

echo "📊 Test avec l'endpoint /user/info"
echo "----------------------------------"

echo "🗑️  Étape 1: Vider le cache d'abord..."
curl -s -X POST "$BASE_URL/cache/evict/all" | jq .

echo ""
echo "🔄 Étape 2: Premier appel (devrait exécuter la méthode et mettre en cache)"
echo "Regardez les logs de votre application - vous devriez voir l'exécution de getInfo"
echo ""
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" | jq '.nomComplet' 2>/dev/null || echo "Requête effectuée (sans token)"

echo ""
echo "🔄 Étape 3: Deuxième appel (devrait utiliser le cache)"
echo "Regardez les logs - vous NE devriez PAS voir l'exécution de getInfo"
echo ""
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" | jq '.nomComplet' 2>/dev/null || echo "Requête effectuée (sans token)"

echo ""
echo "🔄 Étape 4: Troisième appel (devrait utiliser le cache)"
echo "Regardez les logs - vous NE devriez PAS voir l'exécution de getInfo"
echo ""
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" | jq '.nomComplet' 2>/dev/null || echo "Requête effectuée (sans token)"

echo ""
echo "✅ Test terminé!"
echo ""
echo "💡 Interprétation des résultats:"
echo "1. Premier appel: Méthode getInfo exécutée (pas de cache)"
echo "2. Appels suivants: Méthode getInfo PAS exécutée (cache utilisé)"
echo "3. Les temps devraient être similaires car sans token, ça échoue rapidement"
echo ""
echo "🔍 Pour un test complet, remplacez YOUR_TOKEN_HERE par un vrai token JWT"

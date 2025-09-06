#!/bin/bash

echo "🧪 TEST DÉTAILLÉ DU CACHE USER-INFO"
echo "==================================="

BASE_URL="http://localhost:8080/api/auth"

echo "📊 Test avec logs de debug pour vérifier les appels à la base de données"
echo "-----------------------------------------------------------------------"

echo "🗑️  Étape 1: Vider le cache d'abord..."
curl -s -X POST "$BASE_URL/cache/evict/all" | jq .

echo ""
echo "🔄 Étape 2: Premier appel (devrait exécuter getInfo et appeler la DB)"
echo "Regardez les logs de votre application - vous devriez voir:"
echo "🔍 CACHE DEBUG USER-INFO: Exécution de getInfo - UserId: [ID]"
echo ""
echo "Appel en cours..."
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "🔄 Étape 3: Deuxième appel (devrait utiliser le cache, PAS de DB)"
echo "Regardez les logs - vous NE devriez PAS voir le message CACHE DEBUG"
echo ""
echo "Appel en cours..."
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "🔄 Étape 4: Troisième appel (devrait utiliser le cache, PAS de DB)"
echo "Regardez les logs - vous NE devriez PAS voir le message CACHE DEBUG"
echo ""
echo "Appel en cours..."
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "🔄 Étape 5: Appel avec un autre utilisateur (devrait exécuter getInfo)"
echo "Simulation d'un appel avec un autre ID utilisateur..."
echo "Regardez les logs - vous devriez voir un nouveau message CACHE DEBUG"
echo ""
echo "Appel en cours..."
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "✅ Test terminé!"
echo ""
echo "💡 Interprétation des résultats:"
echo "1. Premier appel: Message CACHE DEBUG visible = Méthode exécutée (DB appelée)"
echo "2. Appels 2-3: Pas de message CACHE DEBUG = Cache utilisé (PAS de DB)"
echo "3. Si vous voyez le message à chaque appel, le cache ne fonctionne pas"
echo "4. Si vous ne voyez le message qu'au premier appel, le cache fonctionne parfaitement!"
echo ""
echo "🔍 Vérifiez maintenant les logs de votre application Spring Boot"

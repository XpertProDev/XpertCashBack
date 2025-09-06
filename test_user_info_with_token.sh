#!/bin/bash

echo "🧪 TEST DU CACHE USER-INFO AVEC TOKEN"
echo "====================================="

BASE_URL="http://localhost:8080/api/auth"

echo "📊 Étape 1: Connexion pour obtenir un token"
echo "-------------------------------------------"

# Connexion pour obtenir un token
echo "Connexion en cours..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "carterhedy57@gmail.com",
    "password": "votre_mot_de_passe"
  }')

echo "Réponse de connexion:"
echo "$LOGIN_RESPONSE" | jq .

# Extraire le token
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken // empty')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "❌ Erreur: Impossible d'obtenir un token. Vérifiez vos identifiants."
    echo "💡 Modifiez l'email et le mot de passe dans ce script"
    exit 1
fi

echo ""
echo "✅ Token obtenu: ${TOKEN:0:20}..."

echo ""
echo "🗑️  Étape 2: Vider le cache d'abord..."
curl -s -X POST "$BASE_URL/cache/evict/all" | jq .

echo ""
echo "🔄 Étape 3: Premier appel avec token (devrait exécuter getInfo)"
echo "Regardez les logs - vous devriez voir:"
echo "🔍 CACHE DEBUG USER-INFO: Exécution de getInfo - UserId: [ID]"
echo ""
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.nomComplet' 2>/dev/null || echo "Requête effectuée"

echo ""
echo "🔄 Étape 4: Deuxième appel avec token (devrait utiliser le cache)"
echo "Regardez les logs - vous NE devriez PAS voir le message CACHE DEBUG"
echo ""
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.nomComplet' 2>/dev/null || echo "Requête effectuée"

echo ""
echo "🔄 Étape 5: Troisième appel avec token (devrait utiliser le cache)"
echo "Regardez les logs - vous NE devriez PAS voir le message CACHE DEBUG"
echo ""
time curl -s -X GET "$BASE_URL/user/info" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.nomComplet' 2>/dev/null || echo "Requête effectuée"

echo ""
echo "✅ Test terminé!"
echo ""
echo "💡 Maintenant vérifiez vos logs Spring Boot:"
echo "1. Premier appel: Message CACHE DEBUG visible = DB appelée"
echo "2. Appels suivants: Pas de message CACHE DEBUG = Cache utilisé (PAS de DB)"


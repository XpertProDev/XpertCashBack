#!/bin/bash

# Script de débogage pour l'API de pagination
# Ce script va nous aider à identifier le problème

BASE_URL="http://localhost:8080"
ENTREPRISE_ID="1"

echo "🔍 Débogage de l'API de pagination"
echo "===================================="

echo "📋 Informations de base :"
echo "   - URL de base: $BASE_URL"
echo "   - ID entreprise: $ENTREPRISE_ID"
echo ""

echo "🧪 Test 1: Vérification de la connectivité"
echo "   Test de connexion à l'API..."
curl -s -o /dev/null -w "   Status: %{http_code}\n" "$BASE_URL/actuator/health" 2>/dev/null || echo "   ❌ Impossible de se connecter à l'API"

echo ""

echo "🧪 Test 2: Test sans token (doit retourner 401)"
echo "   Test sans authentification..."
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated" \
  -H "Content-Type: application/json" \
  -w "   Status: %{http_code}\n" \
  -o /tmp/response_no_token.json

echo "   Réponse:"
cat /tmp/response_no_token.json | jq '.' 2>/dev/null || echo "   (Réponse non-JSON ou erreur)"

echo ""

echo "🧪 Test 3: Test avec token invalide"
echo "   Test avec token 'invalid-token'..."
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated" \
  -H "Authorization: Bearer invalid-token" \
  -H "Content-Type: application/json" \
  -w "   Status: %{http_code}\n" \
  -o /tmp/response_invalid_token.json

echo "   Réponse:"
cat /tmp/response_invalid_token.json | jq '.' 2>/dev/null || echo "   (Réponse non-JSON ou erreur)"

echo ""

echo "🧪 Test 4: Test avec token null"
echo "   Test avec token 'null'..."
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated" \
  -H "Authorization: Bearer null" \
  -H "Content-Type: application/json" \
  -w "   Status: %{http_code}\n" \
  -o /tmp/response_null_token.json

echo "   Réponse:"
cat /tmp/response_null_token.json | jq '.' 2>/dev/null || echo "   (Réponse non-JSON ou erreur)"

echo ""

echo "🧪 Test 5: Test avec token vide"
echo "   Test avec token vide..."
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated" \
  -H "Authorization: Bearer " \
  -H "Content-Type: application/json" \
  -w "   Status: %{http_code}\n" \
  -o /tmp/response_empty_token.json

echo "   Réponse:"
cat /tmp/response_empty_token.json | jq '.' 2>/dev/null || echo "   (Réponse non-JSON ou erreur)"

echo ""

echo "🧪 Test 6: Test avec token 'undefined'"
echo "   Test avec token 'undefined'..."
curl -s -X GET "$BASE_URL/api/auth/entreprise/$ENTREPRISE_ID/produits/paginated" \
  -H "Authorization: Bearer undefined" \
  -H "Content-Type: application/json" \
  -w "   Status: %{http_code}\n" \
  -o /tmp/response_undefined_token.json

echo "   Réponse:"
cat /tmp/response_undefined_token.json | jq '.' 2>/dev/null || echo "   (Réponse non-JSON ou erreur)"

echo ""

echo "🔍 Analyse des résultats :"
echo "   - Si le test 1 échoue : Problème de connectivité à l'API"
echo "   - Si les tests 2-6 retournent 200 : Problème de validation côté serveur"
echo "   - Si les tests 2-6 retournent 401 : Problème de token côté client"
echo "   - Si les tests 2-6 retournent 500 : Problème de logique côté serveur"

echo ""

echo "📝 Prochaines étapes :"
echo "   1. Vérifiez que votre application Spring Boot est bien démarrée"
echo "   2. Vérifiez que le port 8080 est bien utilisé"
echo "   3. Vérifiez que votre token JWT est valide et non expiré"
echo "   4. Vérifiez les logs de l'application pour plus de détails"

# Nettoyage des fichiers temporaires
rm -f /tmp/response_*.json

echo ""
echo "✅ Débogage terminé !"



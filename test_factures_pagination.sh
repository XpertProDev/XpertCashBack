#!/bin/bash

# Script de test pour l'API de pagination des factures proforma
# Assurez-vous que votre application Spring Boot est en cours d'exécution

BASE_URL="http://localhost:8080"
TOKEN="your-jwt-token-here"  # Remplacez par votre vrai token JWT
USER_ID="1"  # Remplacez par l'ID de l'utilisateur à tester

echo "🧪 Test de l'API de pagination des factures proforma"
echo "===================================================="

# Test 1: Première page avec taille par défaut (20) - Utilisateur connecté
echo "📄 Test 1: Première page (taille par défaut) - Utilisateur connecté"
curl -s -X GET "$BASE_URL/api/auth/factures/entreprise/paginated" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 2: Première page avec taille par défaut (20) - Utilisateur spécifique
echo "📄 Test 2: Première page (taille par défaut) - Utilisateur spécifique"
curl -s -X GET "$BASE_URL/api/auth/factures/entreprise/$USER_ID/paginated" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 2: Deuxième page avec 15 éléments
echo "📄 Test 2: Deuxième page avec 15 éléments"
curl -s -X GET "$BASE_URL/api/auth/factures/entreprise/$USER_ID/paginated?page=1&size=15" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 3: Page avec taille maximale (100)
echo "📄 Test 3: Page avec taille maximale (100)"
curl -s -X GET "$BASE_URL/api/auth/factures/entreprise/$USER_ID/paginated?page=0&size=100" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 4: Page invalide (négative)
echo "📄 Test 4: Page invalide (négative)"
curl -s -X GET "$BASE_URL/api/auth/factures/entreprise/$USER_ID/paginated?page=-1&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 5: Taille invalide (trop grande)
echo "📄 Test 5: Taille invalide (trop grande)"
curl -s -X GET "$BASE_URL/api/auth/factures/entreprise/$USER_ID/paginated?page=0&size=150" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 6: Sans token d'autorisation
echo "📄 Test 6: Sans token d'autorisation"
curl -s -X GET "$BASE_URL/api/auth/factures/entreprise/$USER_ID/paginated?page=0&size=20" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

# Test 7: Utilisateur inexistant
echo "📄 Test 7: Utilisateur inexistant"
curl -s -X GET "$BASE_URL/api/auth/factures/entreprise/999999/paginated?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n"

echo "✅ Tests terminés !"
echo "📊 Vérifiez les réponses ci-dessus pour vous assurer que la pagination fonctionne correctement."
echo "🔍 Points à vérifier :"
echo "   - Les métadonnées de pagination sont présentes"
echo "   - Le contenu est limité à la taille de page demandée"
echo "   - Les statistiques des factures sont correctes (brouillon, approbation, validées, annulées)"
echo "   - La gestion des erreurs fonctionne"
echo "   - Les droits d'accès sont respectés"
echo "   - Le tri par date de création (décroissant) fonctionne"

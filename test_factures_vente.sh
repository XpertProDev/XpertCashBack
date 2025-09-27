#!/bin/bash

# Script de test pour les endpoints de factures de vente uniquement
# Assurez-vous d'avoir votre token JWT valide

BASE_URL="http://localhost:8080/api/auth"
TOKEN="YOUR_JWT_TOKEN_HERE"  # Remplacez par votre token JWT

echo "🧾 Test des endpoints de factures de vente uniquement"
echo "====================================================="

# Test 1: Toutes les factures de vente de l'entreprise
echo "🏢 Test 1: Toutes les factures de vente de l'entreprise (page=0, size=20)"
curl -X GET "$BASE_URL/factures-vente?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n"

# Test 2: Factures de vente par boutique
echo "🏪 Test 2: Factures de vente pour boutique ID 1 (page=0, size=20)"
curl -X GET "$BASE_URL/factures-vente/boutique/1?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n"

# Test 3: Pagination différente
echo "📄 Test 3: Factures de vente avec pagination (page=1, size=10)"
curl -X GET "$BASE_URL/factures-vente?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n"

# Test 4: Boutique avec pagination différente
echo "📊 Test 4: Boutique ID 1 avec pagination (page=0, size=5)"
curl -X GET "$BASE_URL/factures-vente/boutique/1?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n"

# Test 5: Par défaut (sans paramètres de pagination)
echo "🏢 Test 5: Toutes les factures de vente par défaut (sans paramètres)"
curl -X GET "$BASE_URL/factures-vente" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n"

# Test 6: Boutique par défaut (sans paramètres de pagination)
echo "🏪 Test 6: Boutique ID 1 par défaut (sans paramètres)"
curl -X GET "$BASE_URL/factures-vente/boutique/1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n"

# Test 7: Test avec des IDs différents
echo "🔄 Test 7: Boutique ID 2 (page=0, size=20)"
curl -X GET "$BASE_URL/factures-vente/boutique/2?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n"

# Test 8: Test avec des IDs différents
echo "🔄 Test 8: Boutique ID 3 (page=0, size=20)"
curl -X GET "$BASE_URL/factures-vente/boutique/3?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n"

echo "✅ Tests terminés!"
echo ""
echo "📝 Instructions d'utilisation:"
echo "1. Remplacez YOUR_JWT_TOKEN_HERE par votre token JWT valide"
echo "2. Remplacez les IDs (1, 2, 3) par les vrais IDs de vos boutiques"
echo "3. Assurez-vous que votre serveur Spring Boot est démarré sur le port 8080"
echo "4. Exécutez: chmod +x test_factures_vente.sh && ./test_factures_vente.sh"
echo ""
echo "🔧 Paramètres disponibles:"
echo "- page: Numéro de page (commence à 0, défaut: 0)"
echo "- size: Taille de la page (1-100, défaut: 20)"
echo ""
echo "📋 Nouveaux endpoints disponibles:"
echo "- GET /factures-vente?page=0&size=20"
echo "- GET /factures-vente/boutique/{boutiqueId}?page=0&size=20"
echo ""
echo "💡 Utilisation pratique:"
echo "- Pour voir uniquement les factures de vente (sans dépenses)"
echo "- Pour analyser les ventes par boutique"
echo "- Pour le reporting des ventes uniquement"
echo "- Pour les statistiques de vente"
echo ""
echo "🔒 Sécurité:"
echo "- Les vendeurs ne voient que leurs propres ventes"
echo "- Les admins/managers voient toutes les ventes de l'entreprise"
echo "- Les vendeurs ne peuvent accéder qu'à leur boutique assignée"

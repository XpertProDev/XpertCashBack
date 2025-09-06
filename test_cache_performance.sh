#!/bin/bash

echo "🧪 TEST DE PERFORMANCE DU CACHE"
echo "================================"

# Configuration
BASE_URL="http://localhost:8080/api/auth"
BOUTIQUE_ID=1  # Remplacez par un ID de boutique valide
ENTREPRISE_ID=1  # Remplacez par un ID d'entreprise valide
PRODUIT_ID=1  # Remplacez par un ID de produit valide

echo "📊 Test 1: Cache des produits par boutique"
echo "----------------------------------------"

# Premier appel (devrait être lent - pas de cache)
echo "⏱️  Premier appel (sans cache):"
time curl -s -X GET "$BASE_URL/boutique/$BOUTIQUE_ID/produits/paginated?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Deuxième appel (avec cache):"
time curl -s -X GET "$BASE_URL/boutique/$BOUTIQUE_ID/produits/paginated?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Troisième appel (avec cache):"
time curl -s -X GET "$BASE_URL/boutique/$BOUTIQUE_ID/produits/paginated?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "📊 Test 2: Cache des produits par entreprise"
echo "-------------------------------------------"

echo "⏱️  Premier appel (sans cache):"
time curl -s -X GET "$BASE_URL/entreprise/$ENTREPRISE_ID/produits/paginated?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Deuxième appel (avec cache):"
time curl -s -X GET "$BASE_URL/entreprise/$ENTREPRISE_ID/produits/paginated?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "📊 Test 3: Cache de l'historique des stocks"
echo "------------------------------------------"

echo "⏱️  Premier appel (sans cache):"
time curl -s -X GET "$BASE_URL/stockhistorique/$PRODUIT_ID" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "⏱️  Deuxième appel (avec cache):"
time curl -s -X GET "$BASE_URL/stockhistorique/$PRODUIT_ID" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "📊 Test 4: Invalidation du cache"
echo "-------------------------------"

echo "🗑️  Vidage du cache des produits par boutique:"
curl -s -X POST "$BASE_URL/cache/evict/produits-boutique" \
  -H "Content-Type: application/json"

echo ""
echo "⏱️  Appel après vidage (devrait être lent):"
time curl -s -X GET "$BASE_URL/boutique/$BOUTIQUE_ID/produits/paginated?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "✅ Tests terminés!"
echo ""
echo "💡 Interprétation des résultats:"
echo "- Premier appel: Temps normal (requête DB)"
echo "- Appels suivants: Temps réduit (cache)"
echo "- Après vidage: Temps normal (requête DB)"

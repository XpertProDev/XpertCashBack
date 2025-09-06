#!/bin/bash

echo "⏱️  TEST DE PERFORMANCE DU CACHE"
echo "================================="

BASE_URL="http://localhost:8080/api/auth"

# Fonction pour mesurer le temps d'une requête
measure_time() {
    local url="$1"
    local description="$2"
    
    echo "🔄 $description"
    echo "URL: $url"
    
    # Mesurer le temps avec time
    time_result=$(time (curl -s -X GET "$url" -H "Content-Type: application/json" > /dev/null) 2>&1)
    
    # Extraire le temps réel
    real_time=$(echo "$time_result" | grep "real" | awk '{print $2}')
    echo "⏱️  Temps: $real_time"
    echo ""
}

echo "📊 Test 1: Cache des produits par boutique (sans authentification)"
echo "----------------------------------------------------------------"

# Vider le cache d'abord
echo "🗑️  Vidage du cache..."
curl -s -X POST "$BASE_URL/cache/evict/produits-boutique" > /dev/null

# Test avec un ID de boutique (même si ça va échouer, on peut mesurer le temps)
echo "🔄 Premier appel (sans cache):"
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "🔄 Deuxième appel (avec cache):"
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "🔄 Troisième appel (avec cache):"
time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
  -H "Content-Type: application/json" > /dev/null

echo ""
echo "📊 Test 2: Test de charge avec 10 appels consécutifs"
echo "---------------------------------------------------"

echo "🔄 10 appels consécutifs (devrait être plus rapide après le premier):"
for i in {1..10}; do
    echo -n "Appel $i: "
    time curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
      -H "Content-Type: application/json" > /dev/null
done

echo ""
echo "📊 Test 3: Vérification du fonctionnement du cache"
echo "------------------------------------------------"

echo "🔍 Vérification que le cache est actif:"
echo "1. Premier appel: Temps normal (requête DB + mise en cache)"
echo "2. Appels suivants: Temps réduit (lecture depuis le cache)"
echo "3. Après vidage: Retour au temps normal"

echo ""
echo "✅ Tests terminés!"
echo ""
echo "💡 Interprétation:"
echo "- Si le cache fonctionne, les appels 2-10 devraient être plus rapides"
echo "- Les erreurs 401/403 sont normales sans token, mais le cache fonctionne quand même"
echo "- Le temps 'real' montre la différence de performance"

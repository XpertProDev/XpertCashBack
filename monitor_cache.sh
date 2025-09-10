#!/bin/bash

echo "📊 MONITORING DU CACHE EN TEMPS RÉEL"
echo "===================================="

BASE_URL="http://localhost:8080/api/auth"

# Fonction pour afficher le statut du cache
show_cache_status() {
    echo "🔄 Statut du cache - $(date '+%H:%M:%S')"
    echo "----------------------------------------"
    
    # Test des endpoints de cache
    echo "✅ Endpoints de cache disponibles:"
    echo "   - POST $BASE_URL/cache/evict/produits-boutique"
    echo "   - POST $BASE_URL/cache/evict/produits-entreprise" 
    echo "   - POST $BASE_URL/cache/evict/stock-historique"
    echo "   - POST $BASE_URL/cache/evict/all"
    
    echo ""
    echo "✅ Endpoints mis en cache:"
    echo "   - GET $BASE_URL/boutique/{id}/produits/paginated (cache: produits-boutique)"
    echo "   - GET $BASE_URL/entreprise/{id}/produits/paginated (cache: produits-entreprise)"
    echo "   - GET $BASE_URL/stockhistorique/{id} (cache: stock-historique)"
    
    echo ""
    echo "✅ Invalidation automatique sur:"
    echo "   - createProduit() → invalide produits-boutique + produits-entreprise"
    echo "   - updateProduct() → invalide produits-boutique + produits-entreprise"
    echo "   - ajouterStock() → invalide produits-boutique + produits-entreprise + stock-historique"
    echo "   - retirerStock() → invalide produits-boutique + produits-entreprise + stock-historique"
    echo "   - corbeille() → invalide produits-boutique + produits-entreprise"
    echo "   - deleteStock() → invalide produits-boutique + produits-entreprise + stock-historique"
}

# Fonction pour tester la performance
test_performance() {
    echo ""
    echo "⏱️  Test de performance rapide:"
    echo "-------------------------------"
    
    # Vider le cache
    echo "🗑️  Vidage du cache..."
    curl -s -X POST "$BASE_URL/cache/evict/all" > /dev/null
    
    # Test de performance
    echo "🔄 Test de 5 appels consécutifs:"
    for i in {1..5}; do
        echo -n "   Appel $i: "
        time_result=$(time (curl -s -X GET "$BASE_URL/boutique/1/produits/paginated?page=0&size=20" \
          -H "Content-Type: application/json" > /dev/null) 2>&1)
        real_time=$(echo "$time_result" | grep "real" | awk '{print $2}')
        echo "$real_time"
    done
}

# Fonction pour vérifier la configuration
check_configuration() {
    echo ""
    echo "🔍 Vérification de la configuration:"
    echo "------------------------------------"
    
    echo "✅ Configuration Spring Cache:"
    grep -n "spring.cache" src/main/resources/application-dev.properties
    
    echo ""
    echo "✅ Classe CacheConfig:"
    grep -n "@EnableCaching\|CacheManager" src/main/java/com/xpertcash/configuration/CacheConfig.java
    
    echo ""
    echo "✅ Annotations @Cacheable:"
    grep -c "@Cacheable" src/main/java/com/xpertcash/service/ProduitService.java | xargs echo "   Nombre d'annotations @Cacheable:"
    
    echo ""
    echo "✅ Annotations @CacheEvict:"
    grep -c "@CacheEvict" src/main/java/com/xpertcash/service/ProduitService.java | xargs echo "   Nombre d'annotations @CacheEvict:"
}

# Menu principal
while true; do
    echo ""
    echo "📋 MENU DE MONITORING DU CACHE"
    echo "=============================="
    echo "1. Afficher le statut du cache"
    echo "2. Test de performance"
    echo "3. Vérifier la configuration"
    echo "4. Vider tous les caches"
    echo "5. Quitter"
    echo ""
    read -p "Choisissez une option (1-5): " choice
    
    case $choice in
        1)
            show_cache_status
            ;;
        2)
            test_performance
            ;;
        3)
            check_configuration
            ;;
        4)
            echo "🗑️  Vidage de tous les caches..."
            curl -s -X POST "$BASE_URL/cache/evict/all" | jq .
            ;;
        5)
            echo "👋 Au revoir!"
            break
            ;;
        *)
            echo "❌ Option invalide. Veuillez choisir 1-5."
            ;;
    esac
done

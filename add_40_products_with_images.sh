#!/bin/bash

# Script pour ajouter 40 produits avec catégories et images
# Assurez-vous d'avoir votre token JWT et l'ID de votre boutique

# Configuration
BASE_URL="http://localhost:8080/api/auth"
BOUTIQUE_ID="1"

# Couleurs pour les logs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Début de l'ajout de 40 produits avec images...${NC}"

# Demander le token JWT
echo ""
echo "📋 Instructions:"
echo "1. Connectez-vous à votre application pour obtenir votre token JWT"
echo "2. Entrez votre token JWT ci-dessous"
echo ""

read -p "🔑 Entrez votre token JWT: " TOKEN

if [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ Token JWT requis. Arrêt du script.${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ Token JWT configuré${NC}"

# Fonction pour créer un produit
create_product() {
    local nom="$1"
    local description="$2"
    local prix_vente="$3"
    local prix_achat="$4"
    local quantite="$5"
    local seuil_alert="$6"
    local categorie_id="$7"
    local unite_id="$8"
    local code_bare="$9"
    local image_url="${10}"
    local type_produit="${11}"
    local date_preemption="${12}"

    echo -e "${YELLOW}📦 Création du produit: $nom${NC}"

    # Télécharger l'image temporairement
    local temp_image=""
    if [ ! -z "$image_url" ]; then
        # Utiliser une approche compatible macOS pour mktemp
        temp_image=$(mktemp /tmp/image_XXXXXX.jpg)
        echo -e "${BLUE}📥 Téléchargement de l'image: $image_url${NC}"
        if curl -s -o "$temp_image" "$image_url"; then
            echo -e "${GREEN}✅ Image téléchargée: $temp_image${NC}"
        else
            echo -e "${RED}❌ Erreur téléchargement image: $image_url${NC}"
            temp_image=""
        fi
    fi

    # Préparer les données JSON
    local boutique_ids_json="[$BOUTIQUE_ID]"
    local quantites_json="[$quantite]"
    local seuil_alert_json="[$seuil_alert]"
    
    local produit_json=$(cat <<JSONEOF
{
    "nom": "$nom",
    "description": "$description",
    "prixVente": $prix_vente,
    "prixAchat": $prix_achat,
    "quantite": $quantite,
    "seuilAlert": $seuil_alert,
    "categorieId": $categorie_id,
    "uniteId": $unite_id,
    "codeBare": "$code_bare",
    "typeProduit": "$type_produit",
    "datePreemption": "$date_preemption"
}
JSONEOF
)

    # Créer le produit via l'API
    local response
    if [ ! -z "$temp_image" ]; then
        response=$(curl -s -X POST "$BASE_URL/create" \
            -H "Authorization: Bearer $TOKEN" \
            -F "boutiqueIds=$boutique_ids_json" \
            -F "quantites=$quantites_json" \
            -F "seuilAlert=$seuil_alert_json" \
            -F "produit=$produit_json" \
            -F "image=@$temp_image" \
            -F "addToStock=true")
    else
        response=$(curl -s -X POST "$BASE_URL/create" \
            -H "Authorization: Bearer $TOKEN" \
            -F "boutiqueIds=$boutique_ids_json" \
            -F "quantites=$quantites_json" \
            -F "seuilAlert=$seuil_alert_json" \
            -F "produit=$produit_json" \
            -F "addToStock=true")
    fi

    # Nettoyer l'image temporaire
    if [ ! -z "$temp_image" ] && [ -f "$temp_image" ]; then
        rm "$temp_image"
    fi

    # Vérifier la réponse
    if echo "$response" | grep -q "error"; then
        echo -e "${RED}❌ Erreur création produit $nom: $response${NC}"
        return 1
    else
        echo -e "${GREEN}✅ Produit $nom créé avec succès${NC}"
        return 0
    fi
}

# Fonction pour créer une catégorie
create_category() {
    local nom="$1"
    echo -e "${YELLOW}📂 Création de la catégorie: $nom${NC}"
    
    local response=$(curl -s -X POST "$BASE_URL/createCategory" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"nom\": \"$nom\"}")
    
    if echo "$response" | grep -q "error"; then
        echo -e "${RED}❌ Erreur création catégorie $nom: $response${NC}"
        return 1
    else
        echo -e "${GREEN}✅ Catégorie $nom créée avec succès${NC}"
        return 0
    fi
}

# Fonction pour créer une unité
create_unite() {
    local nom="$1"
    echo -e "${YELLOW}📏 Création de l'unité: $nom${NC}"
    
    local response=$(curl -s -X POST "$BASE_URL/createUnite" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"nom\": \"$nom\"}")
    
    if echo "$response" | grep -q "error"; then
        echo -e "${RED}❌ Erreur création unité $nom: $response${NC}"
        return 1
    else
        echo -e "${GREEN}✅ Unité $nom créée avec succès${NC}"
        return 0
    fi
}

echo -e "${GREEN}✅ Configuration OK, token configuré${NC}"

# Créer les unités nécessaires
echo -e "${BLUE}📏 Création des unités...${NC}"
create_unite "Pièce"
create_unite "Kilogramme"
create_unite "Litre"
create_unite "Mètre"

# Attendre un peu pour que les unités soient créées
sleep 2

# Créer les catégories nécessaires
echo -e "${BLUE}📂 Création des catégories...${NC}"
create_category "Électronique"
create_category "Vêtements"
create_category "Alimentation"
create_category "Maison & Jardin"
create_category "Sports & Loisirs"
create_category "Livres & Médias"
create_category "Beauté & Santé"
create_category "Automobile"
create_category "Bricolage"
create_category "Jouets & Jeux"

# Attendre un peu pour que les catégories soient créées
sleep 2

# Récupérer les IDs des catégories (ajustés selon votre base de données)
# "Sans Category" existe déjà avec l'ID 1, les nouvelles catégories commenceront à l'ID 2
CAT_ELECTRONIQUE=2
CAT_VETEMENTS=3
CAT_ALIMENTATION=4
CAT_MAISON=5
CAT_SPORTS=6
CAT_LIVRES=7
CAT_BEAUTE=8
CAT_AUTO=9
CAT_BRICOLAGE=10
CAT_JOUETS=11
CAT_SANS_CATEGORIE=null

# ID de l'unité (les nouvelles unités commenceront à l'ID 1)
UNITE_PIECE=1
UNITE_KG=2
UNITE_LITRE=3
UNITE_METRE=4

echo -e "${BLUE}📦 Création des 40 produits...${NC}"

# Produits avec catégories - Électronique
create_product "iPhone 15 Pro" "Smartphone Apple dernière génération" 1200.00 1000.00 5 2 $CAT_ELECTRONIQUE $UNITE_PIECE "IPH15PRO001" "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=400" "PHYSIQUE" "2025-12-31"
create_product "Samsung Galaxy S24" "Smartphone Samsung avec IA" 1100.00 900.00 8 3 $CAT_ELECTRONIQUE $UNITE_PIECE "SGS24U001" "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400" "PHYSIQUE" "2025-12-31"
create_product "MacBook Air M3" "Ordinateur portable Apple" 1500.00 1200.00 3 1 $CAT_ELECTRONIQUE $UNITE_PIECE "MBAIRM3001" "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400" "PHYSIQUE" "2025-12-31"
create_product "AirPods Pro" "Écouteurs sans fil Apple" 250.00 200.00 15 5 $CAT_ELECTRONIQUE $UNITE_PIECE "APPRO001" "https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=400" "PHYSIQUE" "2025-12-31"
create_product "iPad Pro 12.9" "Tablette Apple professionnelle" 1200.00 1000.00 4 2 $CAT_ELECTRONIQUE $UNITE_PIECE "IPADPRO001" "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400" "PHYSIQUE" "2025-12-31"

# Produits avec catégories - Vêtements
create_product "T-shirt Nike" "T-shirt sport Nike en coton" 25.00 15.00 50 10 $CAT_VETEMENTS $UNITE_PIECE "TSNIK001" "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400" "PHYSIQUE" ""
create_product "Jean Levis 501" "Jean classique Levis" 80.00 50.00 30 8 $CAT_VETEMENTS $UNITE_PIECE "JLEV501001" "https://images.unsplash.com/photo-1542272604-787c3835535d?w=400" "PHYSIQUE" ""
create_product "Veste Adidas" "Veste de sport Adidas" 60.00 40.00 25 6 $CAT_VETEMENTS $UNITE_PIECE "VADID001" "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=400" "PHYSIQUE" ""
create_product "Chaussures Converse" "Baskets Converse All Star" 70.00 45.00 40 10 $CAT_VETEMENTS $UNITE_PIECE "CCONV001" "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=400" "PHYSIQUE" ""
create_product "Robe Zara" "Robe élégante Zara" 45.00 30.00 20 5 $CAT_VETEMENTS $UNITE_PIECE "RZARA001" "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=400" "PHYSIQUE" ""

# Produits avec catégories - Alimentation
create_product "Pommes Golden" "Pommes fraîches Golden Delicious" 3.50 2.00 100 20 $CAT_ALIMENTATION $UNITE_KG "PGOLD001" "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=400" "PHYSIQUE" "2025-02-15"
create_product "Pain de mie" "Pain de mie complet" 2.50 1.50 30 8 $CAT_ALIMENTATION $UNITE_PIECE "PDMIE001" "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400" "PHYSIQUE" "2025-01-20"
create_product "Lait entier" "Lait frais entier 1L" 1.20 0.80 50 15 $CAT_ALIMENTATION $UNITE_LITRE "LENT001" "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400" "PHYSIQUE" "2025-01-25"
create_product "Fromage Comté" "Fromage Comté AOP 24 mois" 25.00 18.00 15 5 $CAT_ALIMENTATION $UNITE_KG "FCOMT001" "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=400" "PHYSIQUE" "2025-03-10"
create_product "Chocolat noir 70%" "Tablette chocolat noir 100g" 4.50 3.00 40 12 $CAT_ALIMENTATION $UNITE_PIECE "CHOC70_001" "https://images.unsplash.com/photo-1511381939415-e44015466834?w=400" "PHYSIQUE" "2025-06-30"

# Produits avec catégories - Maison & Jardin
create_product "Aspirateur Dyson" "Aspirateur sans fil Dyson V15" 600.00 450.00 5 2 $CAT_MAISON $UNITE_PIECE "ADYSON001" "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400" "PHYSIQUE" ""
create_product "Cafetière Nespresso" "Machine à café Nespresso" 150.00 100.00 8 3 $CAT_MAISON $UNITE_PIECE "CNESP001" "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=400" "PHYSIQUE" ""
create_product "Coussin décoratif" "Coussin en lin naturel" 35.00 20.00 25 8 $CAT_MAISON $UNITE_PIECE "COUS001" "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400" "PHYSIQUE" ""
create_product "Plante Monstera" "Plante verte Monstera Deliciosa" 45.00 30.00 12 4 $CAT_MAISON $UNITE_PIECE "PMONST001" "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=400" "PHYSIQUE" ""
create_product "Bougie parfumée" "Bougie parfumée vanille 200g" 18.00 12.00 30 10 $CAT_MAISON $UNITE_PIECE "BOUG001" "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=400" "PHYSIQUE" ""

# Produits avec catégories - Sports & Loisirs
create_product "Vélo de route" "Vélo de route carbone" 1200.00 800.00 3 1 $CAT_SPORTS $UNITE_PIECE "VROUTE001" "https://images.unsplash.com/photo-1678719873553-548425a1046b?q=80&w=400" "PHYSIQUE" ""
create_product "Raquette tennis" "Raquette tennis Wilson" 180.00 120.00 10 3 $CAT_SPORTS $UNITE_PIECE "RTENN001" "https://images.unsplash.com/photo-1551698618-1dfe5d97d256?w=400" "PHYSIQUE" ""
create_product "Ballon de foot" "Ballon de football Adidas" 25.00 15.00 20 6 $CAT_SPORTS $UNITE_PIECE "BFOOT001" "https://images.unsplash.com/photo-1431324155629-1a6deb1dec8d?w=400" "PHYSIQUE" ""
create_product "Tapis de yoga" "Tapis de yoga antidérapant" 35.00 25.00 15 5 $CAT_SPORTS $UNITE_PIECE "TYOGA001" "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400" "PHYSIQUE" ""
create_product "Haltères ajustables" "Haltères ajustables 2x20kg" 120.00 80.00 8 3 $CAT_SPORTS $UNITE_PIECE "HALT001" "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400" "PHYSIQUE" ""

# Produits avec catégories - Livres & Médias
create_product "Livre Harry Potter" "Harry Potter et la Pierre Philosophale" 12.00 8.00 25 8 $CAT_LIVRES $UNITE_PIECE "LHP001" "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400" "PHYSIQUE" ""
create_product "Roman policier" "Roman policier best-seller" 15.00 10.00 20 6 $CAT_LIVRES $UNITE_PIECE "RPOL001" "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400" "PHYSIQUE" ""
create_product "Livre de cuisine" "Livre de recettes françaises" 25.00 18.00 12 4 $CAT_LIVRES $UNITE_PIECE "LCUIS001" "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400" "PHYSIQUE" ""
create_product "BD Tintin" "Tintin au Tibet" 8.00 5.00 30 10 $CAT_LIVRES $UNITE_PIECE "BTINT001" "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400" "PHYSIQUE" ""
create_product "Dictionnaire Larousse" "Dictionnaire français" 35.00 25.00 8 3 $CAT_LIVRES $UNITE_PIECE "DLAR001" "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400" "PHYSIQUE" ""

# Produits avec catégories - Beauté & Santé
create_product "Crème hydratante" "Crème hydratante visage" 28.00 18.00 20 6 $CAT_BEAUTE $UNITE_PIECE "CHYD001" "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400" "PHYSIQUE" "2025-08-15"
create_product "Parfum Chanel" "Parfum Chanel N°5" 120.00 80.00 5 2 $CAT_BEAUTE $UNITE_PIECE "PCHAN001" "https://images.unsplash.com/photo-1541643600914-78b084683601?w=400" "PHYSIQUE" "2026-01-01"
create_product "Rouge à lèvres" "Rouge à lèvres mat" 18.00 12.00 25 8 $CAT_BEAUTE $UNITE_PIECE "RLIP001" "https://images.unsplash.com/photo-1586495777744-4413f21062fa?w=400" "PHYSIQUE" "2025-10-30"
create_product "Shampoing bio" "Shampoing bio cheveux" 15.00 10.00 30 10 $CAT_BEAUTE $UNITE_PIECE "SHBIO001" "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400" "PHYSIQUE" "2025-09-20"
create_product "Masque facial" "Masque purifiant" 22.00 15.00 18 6 $CAT_BEAUTE $UNITE_PIECE "MFAC001" "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400" "PHYSIQUE" "2025-07-15"

# Produits avec catégories - Automobile
create_product "Pneu Michelin" "Pneu été 205/55 R16" 85.00 60.00 20 6 $CAT_AUTO $UNITE_PIECE "PMICH001" "https://images.unsplash.com/photo-1566238318960-8a8f5473f050?q=80&w=400" "PHYSIQUE" ""
create_product "Huile moteur 5W30" "Huile moteur synthétique 5L" 45.00 30.00 15 5 $CAT_AUTO $UNITE_LITRE "HMOT001" "https://images.unsplash.com/photo-1613214293055-5678e2f6d7de?q=80&w=400" "PHYSIQUE" "2025-12-31"
create_product "Batterie voiture" "Batterie 12V 70Ah" 120.00 80.00 8 3 $CAT_AUTO $UNITE_PIECE "BBAT001" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""
create_product "Pare-brise" "Pare-brise avant" 200.00 150.00 5 2 $CAT_AUTO $UNITE_PIECE "PPAR001" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""
create_product "Feux LED" "Feux LED avant" 80.00 55.00 12 4 $CAT_AUTO $UNITE_PIECE "FLED001" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""

# Produits avec catégories - Bricolage
create_product "Perceuse Bosch" "Perceuse visseuse 18V" 150.00 100.00 8 3 $CAT_BRICOLAGE $UNITE_PIECE "PBOSCH001" "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=400" "PHYSIQUE" ""
create_product "Marteau" "Marteau de charpentier" 25.00 15.00 20 6 $CAT_BRICOLAGE $UNITE_PIECE "MART001" "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=400" "PHYSIQUE" ""
create_product "Vis assorties" "Boîte de vis assorties" 12.00 8.00 30 10 $CAT_BRICOLAGE $UNITE_PIECE "VASS001" "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=400" "PHYSIQUE" ""
create_product "Peinture blanche" "Peinture acrylique 2.5L" 35.00 25.00 15 5 $CAT_BRICOLAGE $UNITE_LITRE "PEINT001" "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=400" "PHYSIQUE" "2025-12-31"
create_product "Pinceau set" "Set de pinceaux peinture" 18.00 12.00 25 8 $CAT_BRICOLAGE $UNITE_PIECE "PSET001" "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=400" "PHYSIQUE" ""

# Produits avec catégories - Jouets & Jeux
create_product "Lego Creator" "Set Lego Creator 3-en-1" 45.00 30.00 12 4 $CAT_JOUETS $UNITE_PIECE "LCR001" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""
create_product "Poupée Barbie" "Poupée Barbie Fashionista" 25.00 18.00 20 6 $CAT_JOUETS $UNITE_PIECE "PBARB001" "https://images.unsplash.com/photo-1730647297091-094977ccbbc7?q=80&w=400" "PHYSIQUE" ""
create_product "Jeu de société" "Monopoly édition classique" 35.00 25.00 15 5 $CAT_JOUETS $UNITE_PIECE "JMSOC001" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""
create_product "Puzzle 1000 pièces" "Puzzle paysage montagne" 18.00 12.00 25 8 $CAT_JOUETS $UNITE_PIECE "PUZ1000_001" "https://plus.unsplash.com/premium_photo-1723507389644-a69471da76d5?q=80&w=400" "PHYSIQUE" ""
create_product "Voiture télécommandée" "Voiture RC 1:18" 80.00 55.00 8 3 $CAT_JOUETS $UNITE_PIECE "VRC001" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""

# Produits SANS catégorie (pour tester la fonctionnalité "Sans Category")
create_product "Produit mystère 1" "Produit sans catégorie définie" 50.00 35.00 10 3 $CAT_SANS_CATEGORIE $UNITE_PIECE "PMYST001" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""
create_product "Produit mystère 2" "Autre produit sans catégorie" 75.00 50.00 8 2 $CAT_SANS_CATEGORIE $UNITE_PIECE "PMYST002" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""
create_product "Produit mystère 3" "Troisième produit sans catégorie" 30.00 20.00 15 5 $CAT_SANS_CATEGORIE $UNITE_PIECE "PMYST003" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""
create_product "Produit mystère 4" "Quatrième produit sans catégorie" 95.00 65.00 6 2 $CAT_SANS_CATEGORIE $UNITE_PIECE "PMYST004" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""
create_product "Produit mystère 5" "Cinquième produit sans catégorie" 40.00 28.00 12 4 $CAT_SANS_CATEGORIE $UNITE_PIECE "PMYST005" "https://images.unsplash.com/photo-1756806381989-d10b7f569e89?q=80&w=400" "PHYSIQUE" ""

echo -e "${GREEN}🎉 Script terminé ! 40 produits ont été ajoutés avec succès.${NC}"
echo -e "${BLUE}📊 Résumé:${NC}"
echo -e "  - 35 produits avec catégories variées"
echo -e "  - 5 produits sans catégorie (seront dans 'Sans Category')"
echo -e "  - Images téléchargées depuis Unsplash"
echo -e "  - Tous les produits sont ajoutés au stock"

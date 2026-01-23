#!/bin/bash

# Script to add 40 products with images to XpertCashBack
# Usage: ./add_40_products_with_images.sh <JWT_TOKEN> <BOUTIQUE_ID> [BASE_URL]
# Example: ./add_40_products_with_images.sh "your_jwt_token_here" 1 "http://localhost:8080"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if required parameters are provided
if [ $# -lt 2 ]; then
    echo -e "${RED}Error: Missing required parameters${NC}"
    echo "Usage: $0 <JWT_TOKEN> <BOUTIQUE_ID> [BASE_URL]"
    echo "Example: $0 \"your_jwt_token\" 1 \"http://localhost:8080\""
    exit 1
fi

JWT_TOKEN="$1"
BOUTIQUE_ID="$2"
BASE_URL="${3:-http://localhost:8080}"

# Validate JWT token format
if [[ ! "$JWT_TOKEN" =~ ^Bearer\ .+ ]] && [[ ! "$JWT_TOKEN" =~ ^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+ ]]; then
    echo -e "${YELLOW}Warning: JWT token doesn't look like a valid token. Continuing anyway...${NC}"
fi

# Add Bearer prefix if not present
if [[ ! "$JWT_TOKEN" =~ ^Bearer\ .+ ]]; then
    JWT_TOKEN="Bearer $JWT_TOKEN"
fi

# Path to images directory
IMAGES_DIR="src/main/resources/static/uploads"

# Check if images directory exists
if [ ! -d "$IMAGES_DIR" ]; then
    echo -e "${RED}Error: Images directory not found: $IMAGES_DIR${NC}"
    exit 1
fi

# Get list of image files
IMAGES=($(find "$IMAGES_DIR" -type f \( -name "*.jpg" -o -name "*.png" -o -name "*.jpeg" \) | head -40))

if [ ${#IMAGES[@]} -eq 0 ]; then
    echo -e "${RED}Error: No images found in $IMAGES_DIR${NC}"
    exit 1
fi

echo -e "${GREEN}Found ${#IMAGES[@]} images${NC}"
echo -e "${GREEN}Starting to add products...${NC}"
echo ""

# Product names and descriptions (40 different products)
PRODUCT_NAMES=(
    "Ordinateur Portable Dell"
    "Smartphone Samsung Galaxy"
    "Tablette iPad Pro"
    "Casque Audio Sony"
    "Souris Logitech"
    "Clavier Mécanique"
    "Écran 27 pouces"
    "Webcam HD 1080p"
    "Microphone USB"
    "Enceinte Bluetooth"
    "Chargeur Sans Fil"
    "Power Bank 20000mAh"
    "Câble USB-C"
    "Adaptateur HDMI"
    "Disque Dur Externe 1TB"
    "Clé USB 64GB"
    "Carte Mémoire SD"
    "Étui pour Smartphone"
    "Protection Écran"
    "Support pour Ordinateur"
    "Lampe de Bureau LED"
    "Tapis de Souris"
    "Hub USB-C"
    "Station d'Accueil"
    "Imprimante HP"
    "Scanner Canon"
    "Routeur WiFi 6"
    "Switch Ethernet"
    "Câble Réseau Cat6"
    "Antenne WiFi"
    "Batterie Externe"
    "Refroidisseur Laptop"
    "Haut-parleurs 2.1"
    "Casque Gaming"
    "Manette Xbox"
    "Manette PlayStation"
    "Clavier Gaming RGB"
    "Souris Gaming"
    "Tapis Gaming"
    "Moniteur 4K"
)

PRODUCT_DESCRIPTIONS=(
    "Ordinateur portable haute performance avec processeur Intel i7"
    "Smartphone Android avec écran AMOLED 6.5 pouces"
    "Tablette Apple avec écran Retina et Apple Pencil compatible"
    "Casque audio sans fil avec réduction de bruit active"
    "Souris ergonomique sans fil avec capteur haute précision"
    "Clavier mécanique avec switches Cherry MX"
    "Écran IPS 27 pouces avec résolution QHD"
    "Webcam Full HD avec micro intégré"
    "Microphone USB avec réduction de bruit"
    "Enceinte Bluetooth portable avec batterie longue durée"
    "Chargeur sans fil compatible Qi"
    "Power Bank haute capacité pour smartphones et tablettes"
    "Câble USB-C haute vitesse de transfert"
    "Adaptateur HDMI vers USB-C"
    "Disque dur externe portable 1TB"
    "Clé USB 3.0 haute vitesse 64GB"
    "Carte mémoire SDXC classe 10"
    "Étui de protection en silicone"
    "Protection d'écran en verre trempé"
    "Support réglable pour ordinateur portable"
    "Lampe de bureau LED avec contrôle de luminosité"
    "Tapis de souris avec surface lisse"
    "Hub USB-C avec ports multiples"
    "Station d'accueil pour ordinateur portable"
    "Imprimante multifonction HP"
    "Scanner de documents haute résolution"
    "Routeur WiFi 6 avec portée étendue"
    "Switch réseau 8 ports Gigabit"
    "Câble réseau Ethernet Cat6"
    "Antenne WiFi directionnelle"
    "Batterie externe portable"
    "Refroidisseur pour ordinateur portable"
    "Haut-parleurs stéréo avec subwoofer"
    "Casque gaming avec son surround 7.1"
    "Manette Xbox sans fil"
    "Manette PlayStation DualSense"
    "Clavier gaming mécanique RGB"
    "Souris gaming avec capteur optique haute précision"
    "Tapis de souris gaming XXL"
    "Moniteur 4K UHD avec HDR"
)

# Prices (random between 5000 and 50000)
PRICES_VENTE=(10000 25000 35000 15000 5000 12000 45000 8000 10000 12000 5000 8000 3000 5000 15000 5000 4000 2000 3000 5000 8000 2000 6000 20000 30000 25000 20000 15000 5000 12000 10000 8000 18000 25000 15000 18000 20000 12000 5000 40000)
PRICES_ACHAT=(5000 15000 20000 8000 2500 6000 25000 4000 5000 6000 2500 4000 1500 2500 8000 2500 2000 1000 1500 2500 4000 1000 3000 10000 15000 12000 10000 8000 2500 6000 5000 4000 9000 12000 8000 9000 10000 6000 2500 20000)

# Quantities (random between 10 and 100)
QUANTITIES=(50 30 20 40 60 35 15 45 25 30 50 40 80 60 25 70 90 100 75 50 40 60 30 20 15 25 30 40 50 35 45 40 25 30 20 25 30 40 50 10)

# Seuil Alert (random between 5 and 20)
SEUIL_ALERT=(10 5 5 10 15 8 5 10 8 10 15 10 20 15 8 20 25 30 20 15 10 15 8 5 5 8 10 15 20 10 12 10 8 10 5 8 10 15 20 5)

SUCCESS_COUNT=0
FAILED_COUNT=0

# Loop through 40 products
for i in {0..39}; do
    PRODUCT_NAME="${PRODUCT_NAMES[$i]}"
    PRODUCT_DESC="${PRODUCT_DESCRIPTIONS[$i]}"
    IMAGE_PATH="${IMAGES[$i]}"
    PRIX_VENTE="${PRICES_VENTE[$i]}"
    PRIX_ACHAT="${PRICES_ACHAT[$i]}"
    QUANTITE="${QUANTITIES[$i]}"
    SEUIL="${SEUIL_ALERT[$i]}"
    
    # Generate a unique barcode
    CODE_BARE="BC$(printf "%08d" $((i + 1)))"
    
    # Create JSON for produit
    PRODUIT_JSON=$(cat <<EOF
{
  "nom": "$PRODUCT_NAME",
  "description": "$PRODUCT_DESC",
  "prixVente": $PRIX_VENTE,
  "prixAchat": $PRIX_ACHAT,
  "codeBare": "$CODE_BARE",
  "typeProduit": "PHYSIQUE"
}
EOF
)
    
    # Create JSON arrays
    BOUTIQUE_IDS_JSON="[$BOUTIQUE_ID]"
    QUANTITES_JSON="[$QUANTITE]"
    SEUIL_ALERT_JSON="[$SEUIL]"
    
    echo -e "${YELLOW}[$((i+1))/40] Adding: $PRODUCT_NAME${NC}"
    
    # Make the API call using curl
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/create" \
        -H "Authorization: $JWT_TOKEN" \
        -H "Content-Type: multipart/form-data" \
        -F "boutiqueIds=$BOUTIQUE_IDS_JSON" \
        -F "quantites=$QUANTITES_JSON" \
        -F "produit=$PRODUIT_JSON" \
        -F "seuilAlert=$SEUIL_ALERT_JSON" \
        -F "image=@$IMAGE_PATH" \
        -F "addToStock=true")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✓ Success (HTTP $HTTP_CODE)${NC}"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo -e "${RED}✗ Failed (HTTP $HTTP_CODE)${NC}"
        echo "Response: $BODY"
        FAILED_COUNT=$((FAILED_COUNT + 1))
    fi
    
    echo ""
    
    # Small delay to avoid overwhelming the server
    sleep 0.5
done

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Summary:${NC}"
echo -e "${GREEN}Success: $SUCCESS_COUNT${NC}"
echo -e "${RED}Failed: $FAILED_COUNT${NC}"
echo -e "${GREEN}========================================${NC}"

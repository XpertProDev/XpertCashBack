#!/bin/bash

# Script pour monitorer l'utilisation réelle des APIs
# Ce script va analyser les logs et créer un rapport d'utilisation

echo "📊 Monitoring de l'Utilisation Réelle des APIs"
echo "=============================================="
echo ""

# Couleurs pour les logs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

LOG_FILE="logs/xpertcash.log"
REPORT_FILE="real_api_usage_report.md"

# Vérifier si le fichier de log existe
if [ ! -f "$LOG_FILE" ]; then
    echo -e "${RED}❌ Fichier de log non trouvé: $LOG_FILE${NC}"
    echo -e "${YELLOW}💡 Assurez-vous que votre application est démarrée et génère des logs${NC}"
    exit 1
fi

echo -e "${BLUE}🔍 Analyse des logs d'accès...${NC}"

# Extraire les requêtes HTTP des logs
echo "📋 Extraction des requêtes HTTP..."

# Créer le rapport
cat > "$REPORT_FILE" << 'EOF'
# Rapport d'Utilisation Réelle des APIs

## 📅 Date: $(date)

## 🔍 Méthode d'Analyse
Ce rapport analyse les logs d'accès du serveur pour identifier les APIs réellement utilisées.

## 📊 Statistiques d'Utilisation

### Requêtes par Méthode HTTP
EOF

# Analyser les méthodes HTTP
echo -e "${YELLOW}📈 Analyse des méthodes HTTP...${NC}"
echo "### Méthodes HTTP Utilisées" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Compter les méthodes HTTP
GET_COUNT=$(grep -c "GET" "$LOG_FILE" 2>/dev/null || echo "0")
POST_COUNT=$(grep -c "POST" "$LOG_FILE" 2>/dev/null || echo "0")
PUT_COUNT=$(grep -c "PUT" "$LOG_FILE" 2>/dev/null || echo "0")
DELETE_COUNT=$(grep -c "DELETE" "$LOG_FILE" 2>/dev/null || echo "0")
PATCH_COUNT=$(grep -c "PATCH" "$LOG_FILE" 2>/dev/null || echo "0")

echo "- GET: $GET_COUNT requêtes" >> "$REPORT_FILE"
echo "- POST: $POST_COUNT requêtes" >> "$REPORT_FILE"
echo "- PUT: $PUT_COUNT requêtes" >> "$REPORT_FILE"
echo "- DELETE: $DELETE_COUNT requêtes" >> "$REPORT_FILE"
echo "- PATCH: $PATCH_COUNT requêtes" >> "$REPORT_FILE"

echo -e "${GREEN}✅ GET: $GET_COUNT requêtes${NC}"
echo -e "${GREEN}✅ POST: $POST_COUNT requêtes${NC}"
echo -e "${GREEN}✅ PUT: $PUT_COUNT requêtes${NC}"
echo -e "${GREEN}✅ DELETE: $DELETE_COUNT requêtes${NC}"
echo -e "${GREEN}✅ PATCH: $PATCH_COUNT requêtes${NC}"

# Extraire les endpoints uniques
echo ""
echo -e "${YELLOW}🔍 Extraction des endpoints uniques...${NC}"

# Créer un fichier temporaire pour les endpoints
TEMP_ENDPOINTS="temp_endpoints.txt"

# Extraire les endpoints des logs (pattern simplifié)
grep -oE "(GET|POST|PUT|DELETE|PATCH) [^ ]*" "$LOG_FILE" | sort | uniq -c | sort -nr > "$TEMP_ENDPOINTS"

echo "" >> "$REPORT_FILE"
echo "### Endpoints les Plus Utilisés" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Afficher les 20 endpoints les plus utilisés
echo -e "${BLUE}📊 Top 20 des endpoints les plus utilisés:${NC}"
head -20 "$TEMP_ENDPOINTS" | while read count endpoint; do
    echo -e "${GREEN}✅ $count x $endpoint${NC}"
    echo "- $count x \`$endpoint\`" >> "$REPORT_FILE"
done

# Analyser les endpoints non utilisés
echo ""
echo -e "${YELLOW}🔍 Analyse des endpoints potentiellement non utilisés...${NC}"

# Créer une liste de tous les endpoints définis dans les contrôleurs
ALL_ENDPOINTS="all_endpoints.txt"
echo "" > "$ALL_ENDPOINTS"

# Extraire tous les endpoints des contrôleurs
find src/main/java/com/xpertcash/controller -name "*.java" -exec grep -oE "@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping).*" {} \; | \
sed 's/.*@[A-Za-z]*Mapping.*("\([^"]*\)").*/\1/' | \
sed 's/.*@[A-Za-z]*Mapping.*value.*=.*"\([^"]*\)".*/\1/' | \
grep -v "^$" | sort | uniq > "$ALL_ENDPOINTS"

echo "" >> "$REPORT_FILE"
echo "### Endpoints Potentiellement Non Utilisés" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Comparer avec les endpoints utilisés
USED_ENDPOINTS="used_endpoints.txt"
grep -oE "(GET|POST|PUT|DELETE|PATCH) [^ ]*" "$LOG_FILE" | sed 's/^[A-Z]* //' | sort | uniq > "$USED_ENDPOINTS"

echo -e "${RED}❌ Endpoints potentiellement non utilisés:${NC}"
while read endpoint; do
    if [ ! -z "$endpoint" ] && ! grep -q "^$endpoint$" "$USED_ENDPOINTS"; then
        echo -e "${RED}❌ $endpoint${NC}"
        echo "- \`$endpoint\`" >> "$REPORT_FILE"
    fi
done < "$ALL_ENDPOINTS"

# Nettoyer les fichiers temporaires
rm -f "$TEMP_ENDPOINTS" "$ALL_ENDPOINTS" "$USED_ENDPOINTS"

echo ""
echo -e "${GREEN}✅ Rapport généré: $REPORT_FILE${NC}"

# Ajouter des recommandations au rapport
cat >> "$REPORT_FILE" << 'EOF'

## 🎯 Recommandations

### Basé sur l'Analyse des Logs
1. **Endpoints Non Utilisés**: Supprimer les endpoints qui n'apparaissent pas dans les logs
2. **Endpoints Peu Utilisés**: Vérifier si ces endpoints sont nécessaires
3. **Endpoints Très Utilisés**: Optimiser les performances de ces endpoints

### Actions Recommandées
1. **Immédiat**: Supprimer les endpoints non utilisés confirmés
2. **Court terme**: Analyser les endpoints peu utilisés
3. **Long terme**: Implémenter un monitoring continu

## 📈 Prochaines Étapes
1. Examiner ce rapport
2. Confirmer avec l'équipe frontend
3. Supprimer les endpoints non utilisés
4. Mettre en place un monitoring continu

EOF

echo ""
echo -e "${BLUE}📋 Prochaines étapes:${NC}"
echo "1. Examiner le rapport: $REPORT_FILE"
echo "2. Comparer avec l'analyse précédente"
echo "3. Confirmer les endpoints à supprimer"
echo "4. Exécuter le nettoyage"
echo ""
echo -e "${YELLOW}💡 Pour un monitoring continu:${NC}"
echo "- Activer les logs d'accès détaillés"
echo "- Utiliser Spring Boot Actuator"
echo "- Implémenter un système de métriques"

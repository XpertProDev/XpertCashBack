#!/bin/bash

# Script de test simple
echo "🧪 Test du script d'ajout de produits"
echo "====================================="

# Vérifier que les fichiers existent
echo "📁 Vérification des fichiers:"
if [ -f "add_40_products_with_images.sh" ]; then
    echo "✅ add_40_products_with_images.sh - OK"
else
    echo "❌ add_40_products_with_images.sh - MANQUANT"
fi

if [ -f "get_api_info.sh" ]; then
    echo "✅ get_api_info.sh - OK"
else
    echo "❌ get_api_info.sh - MANQUANT"
fi

if [ -f "config_products.sh" ]; then
    echo "✅ config_products.sh - OK"
else
    echo "❌ config_products.sh - MANQUANT"
fi

if [ -f "README_PRODUCTS_SCRIPT.md" ]; then
    echo "✅ README_PRODUCTS_SCRIPT.md - OK"
else
    echo "❌ README_PRODUCTS_SCRIPT.md - MANQUANT"
fi

echo ""
echo "🔧 Vérification des permissions:"
ls -la *.sh

echo ""
echo "📋 Instructions d'utilisation:"
echo "1. Configurez votre token JWT dans add_40_products_with_images.sh"
echo "2. Ajustez l'ID de votre boutique"
echo "3. Exécutez: ./add_40_products_with_images.sh"
echo ""
echo "💡 Pour obtenir les informations API: ./get_api_info.sh"

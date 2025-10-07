# 🔒 Guide de Sécurité - XpertCashBack

## ⚠️ IMPORTANT - Configuration Sécurisée

Ce projet utilise maintenant des **variables d'environnement** pour sécuriser les informations sensibles.

## 🚀 Installation et Configuration

### 1. Créer le fichier .env
```bash
# Copier le fichier d'exemple
cp .env.example .env

# Éditer avec vos vraies valeurs
nano .env
```

### 2. Variables d'environnement requises

#### Base de Données
```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=xpertCash_db
DB_USERNAME=root
DB_PASSWORD=votre_mot_de_passe_db
```

#### Production Base de Données
```bash
DB_PROD_USERNAME=xpert_db
DB_PROD_PASSWORD=votre_mot_de_passe_production_securise
```

#### Email (Gmail)
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=votre_email@gmail.com
MAIL_PASSWORD=votre_mot_de_passe_application_gmail
```

#### JWT Secret (CRITIQUE)
```bash
# Générer un secret sécurisé de 256 bits minimum
JWT_SECRET=votre_secret_jwt_tres_long_et_aleatoire_256_bits_minimum
```

### 3. Génération d'un JWT Secret sécurisé

```bash
# Option 1: OpenSSL
openssl rand -base64 32

# Option 2: Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Option 3: Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

## 🔐 Bonnes Pratiques de Sécurité

### ✅ À FAIRE
- ✅ Utiliser des mots de passe forts (minimum 12 caractères)
- ✅ Activer l'authentification à 2 facteurs sur Gmail
- ✅ Utiliser des mots de passe d'application Gmail (pas le mot de passe principal)
- ✅ Changer le JWT secret en production
- ✅ Ne jamais commiter le fichier `.env`
- ✅ Utiliser HTTPS en production

### ❌ À ÉVITER
- ❌ Ne jamais mettre de secrets en dur dans le code
- ❌ Ne jamais commiter le fichier `.env`
- ❌ Ne pas utiliser le même JWT secret en dev et prod
- ❌ Ne pas utiliser des mots de passe faibles
- ❌ Ne pas exposer les logs de debug en production

## 🚨 En cas de compromission

Si vous suspectez une compromission :

1. **Changer immédiatement** :
   - Mot de passe de la base de données
   - Mot de passe Gmail
   - JWT secret

2. **Révoquer** :
   - Tous les tokens JWT existants
   - Sessions utilisateurs

3. **Auditer** :
   - Logs d'accès
   - Activité suspecte

## 📋 Checklist de Déploiement

### Développement
- [ ] Fichier `.env` créé avec les bonnes valeurs
- [ ] JWT secret généré de manière sécurisée
- [ ] Base de données locale configurée
- [ ] Email de test configuré

### Production
- [ ] Variables d'environnement configurées sur le serveur
- [ ] JWT secret différent de celui de développement
- [ ] Base de données sécurisée avec utilisateur dédié
- [ ] HTTPS activé
- [ ] Logs de debug désactivés
- [ ] Swagger désactivé
- [ ] Firewall configuré
- [ ] Sauvegardes automatiques activées

## 🆘 Support

En cas de problème de sécurité, contactez immédiatement l'équipe de développement.

---
**Dernière mise à jour** : $(date)
**Version** : 1.0

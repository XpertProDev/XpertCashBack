# 🔧 Guide de Résolution des Problèmes - API Pagination

## 🚨 Erreur : "The given id must not be null"

### 📋 Description du problème
L'erreur "The given id must not be null" indique qu'un des IDs utilisés dans la requête est null. Cette erreur peut survenir à plusieurs endroits.

### 🔍 Diagnostic

#### 1. **Vérification du token JWT**
```bash
# Testez votre token avec ce script
chmod +x debug_api.sh
./debug_api.sh
```

#### 2. **Vérification des logs de l'application**
Regardez les logs de votre application Spring Boot pour identifier exactement où l'erreur se produit.

#### 3. **Vérification de la base de données**
Assurez-vous que :
- L'utilisateur existe dans la base de données
- L'utilisateur a une entreprise associée
- L'entreprise a un ID valide
- La boutique existe et est active

### 🛠️ Solutions possibles

#### **Problème 1 : Token JWT invalide ou expiré**
```bash
# Vérifiez que votre token est valide
curl -X GET "http://localhost:8080/api/auth/entreprise/1/produits/paginated" \
  -H "Authorization: Bearer VOTRE_VRAI_TOKEN_ICI" \
  -H "Content-Type: application/json"
```

#### **Problème 2 : Utilisateur sans entreprise**
```sql
-- Vérifiez dans votre base de données
SELECT u.id, u.username, e.id as entreprise_id, e.nom as entreprise_nom
FROM users u
LEFT JOIN entreprise e ON u.entreprise_id = e.id
WHERE u.id = VOTRE_USER_ID;
```

#### **Problème 3 : Entreprise sans ID**
```sql
-- Vérifiez que l'entreprise a un ID valide
SELECT id, nom, actif
FROM entreprise
WHERE id = 1;
```

#### **Problème 4 : Problème de configuration JWT**
Vérifiez votre fichier `application.properties` :
```properties
# Assurez-vous que ces propriétés sont définies
jwt.secret=votre_secret_jwt
jwt.expiration=86400000
```

### 🧪 Tests de diagnostic

#### **Test 1 : Vérification de la connectivité**
```bash
curl -X GET "http://localhost:8080/actuator/health"
```

#### **Test 2 : Test sans authentification**
```bash
curl -X GET "http://localhost:8080/api/auth/entreprise/1/produits/paginated"
# Doit retourner 401 Unauthorized
```

#### **Test 3 : Test avec token invalide**
```bash
curl -X GET "http://localhost:8080/api/auth/entreprise/1/produits/paginated" \
  -H "Authorization: Bearer invalid-token"
# Doit retourner une erreur appropriée
```

### 🔧 Corrections dans le code

J'ai déjà ajouté des validations supplémentaires dans le code :

1. **Validation de l'ID de l'entreprise**
2. **Validation de l'ID utilisateur extrait du token**
3. **Validation de l'entreprise de l'utilisateur**
4. **Messages d'erreur plus détaillés**

### 📝 Étapes de résolution

1. **Exécutez le script de débogage**
   ```bash
   ./debug_api.sh
   ```

2. **Vérifiez les logs de l'application**
   - Regardez les erreurs détaillées
   - Identifiez l'étape exacte où l'erreur se produit

3. **Vérifiez votre token JWT**
   - Assurez-vous qu'il n'est pas expiré
   - Vérifiez qu'il contient bien l'ID utilisateur

4. **Vérifiez la base de données**
   - Confirmez que l'utilisateur existe
   - Confirmez que l'utilisateur a une entreprise
   - Confirmez que l'entreprise a un ID valide

5. **Testez avec un token valide**
   - Connectez-vous à votre application
   - Récupérez un nouveau token
   - Testez l'API avec ce token

### 🚀 Test après correction

Une fois le problème résolu, testez avec :

```bash
# Test de base
curl -X GET "http://localhost:8080/api/auth/entreprise/1/produits/paginated" \
  -H "Authorization: Bearer VOTRE_TOKEN_VALIDE"

# Test avec pagination
curl -X GET "http://localhost:8080/api/auth/entreprise/1/produits/paginated?page=0&size=10" \
  -H "Authorization: Bearer VOTRE_TOKEN_VALIDE"
```

### 📞 Support

Si le problème persiste après avoir suivi ce guide :

1. **Partagez les logs complets** de l'application
2. **Partagez la réponse exacte** de l'API
3. **Décrivez les étapes** que vous avez suivies
4. **Indiquez votre environnement** (OS, version Java, etc.)

### 🔍 Points de vérification rapides

- [ ] Application Spring Boot démarrée sur le port 8080
- [ ] Token JWT valide et non expiré
- [ ] Utilisateur existe dans la base de données
- [ ] Utilisateur a une entreprise associée
- [ ] Entreprise a un ID valide
- [ ] Base de données accessible et fonctionnelle




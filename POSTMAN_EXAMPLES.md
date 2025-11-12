# Exemples de Requêtes Postman - Gestion des Dépenses Générales

## Configuration de Base
- **Base URL**: `http://localhost:8080/api/auth` (ajuster selon votre configuration)
- **Headers requis**: 
  - `Content-Type: application/json`
  - `Authorization: Bearer {votre_token_jwt}`

---

## 1. Créer une Catégorie de Dépense

### Endpoint
```
POST /api/auth/comptabilite/categories-depense
```

### Headers
```
Content-Type: application/json
Authorization: Bearer {votre_token_jwt}
```

### Body (JSON)
```json
{
  "nom": "Fournitures de bureau",
  "description": "Catégorie pour les dépenses liées aux fournitures de bureau"
}
```

### Exemple de Réponse
```json
{
  "id": 1,
  "nom": "Fournitures de bureau",
  "description": "Catégorie pour les dépenses liées aux fournitures de bureau",
  "entrepriseId": 1,
  "createdAt": "2025-01-15T10:30:00"
}
```

---

## 2. Lister toutes les Catégories de Dépense

### Endpoint
```
GET /api/auth/comptabilite/categories-depense
```

### Headers
```
Authorization: Bearer {votre_token_jwt}
```

### Exemple de Réponse
```json
[
  {
    "id": 1,
    "nom": "Fournitures de bureau",
    "description": "Catégorie pour les dépenses liées aux fournitures de bureau",
    "entrepriseId": 1,
    "createdAt": "2025-01-15T10:30:00"
  },
  {
    "id": 2,
    "nom": "Transport",
    "description": "Dépenses de transport",
    "entrepriseId": 1,
    "createdAt": "2025-01-15T11:00:00"
  }
]
```

---

## 3. Créer une Dépense Générale

### Endpoint
```
POST /api/auth/comptabilite/depenses-generales
```

### Headers
```
Content-Type: multipart/form-data
Authorization: Bearer {votre_token_jwt}
```

**⚠️ IMPORTANT:** Cet endpoint utilise `multipart/form-data` pour permettre l'upload de pièces jointes (images, PDF, etc.).

### Format de la Requête (multipart/form-data)

Dans Postman, utilisez l'onglet **"Body"** → **"form-data"** avec les champs suivants :

### Exemple 1: Dépense avec catégorie existante (sans pièce jointe)

**Dans Postman, configurez comme suit :**

| Key | Type | Value |
|-----|------|-------|
| depense | Text | `{"designation":"Achat de papeterie","categorieId":1,"prixUnitaire":5000.0,"quantite":10,"source":"CAISSE","ordonnateur":"COMPTABLE","numeroCheque":null,"typeCharge":"CHARGE_VARIABLE","produitId":null,"fournisseurId":1,"pieceJointe":null}` |
| pieceJointe | File | (Laisser vide ou ne pas inclure) |

### Exemple 2: Dépense avec création d'une nouvelle catégorie (sans pièce jointe)

| Key | Type | Value |
|-----|------|-------|
| depense | Text | `{"designation":"Frais de transport","categorieId":null,"nouvelleCategorieNom":"Transport","prixUnitaire":2500.0,"quantite":5,"source":"MOBILE_MONEY","ordonnateur":"MANAGER","numeroCheque":null,"typeCharge":"CHARGE_VARIABLE","produitId":null,"fournisseurId":null,"pieceJointe":null}` |
| pieceJointe | File | (Laisser vide ou ne pas inclure) |

### Exemple 3: Dépense complète avec tous les champs (sans pièce jointe)

| Key | Type | Value |
|-----|------|-------|
| depense | Text | `{"designation":"Achat de matériel informatique","categorieId":1,"prixUnitaire":150000.0,"quantite":2,"source":"BANQUE","ordonnateur":"COMPTABLE","numeroCheque":"CHQ-001234","typeCharge":"CHARGE_FIXE","produitId":5,"fournisseurId":3,"pieceJointe":null}` |
| pieceJointe | File | (Laisser vide ou ne pas inclure) |

### Exemple 4: Dépense avec dette (sans pièce jointe)

| Key | Type | Value |
|-----|------|-------|
| depense | Text | `{"designation":"Facture d'électricité","categorieId":2,"prixUnitaire":75000.0,"quantite":1,"source":"DETTE","ordonnateur":"MANAGER","numeroCheque":null,"typeCharge":"CHARGE_FIXE","produitId":null,"fournisseurId":2,"pieceJointe":null}` |
| pieceJointe | File | (Laisser vide ou ne pas inclure) |

### Exemple 5: Dépense avec pièce jointe (IMAGE/PDF) ⭐

**Dans Postman, configurez comme suit :**

| Key | Type | Value |
|-----|------|-------|
| depense | Text | `{"designation":"Achat de matériel informatique","categorieId":1,"prixUnitaire":150000.0,"quantite":2,"source":"BANQUE","ordonnateur":"COMPTABLE","numeroCheque":"CHQ-001234","typeCharge":"CHARGE_FIXE","produitId":5,"fournisseurId":3,"pieceJointe":null}` |
| pieceJointe | File | [Sélectionner votre fichier] |

**⚠️ IMPORTANT pour l'upload de fichier :**
- Le champ `pieceJointe` doit être de type **File** (pas Text)
- Cliquez sur le champ `pieceJointe` et sélectionnez **"File"** dans le menu déroulant à droite
- Cliquez sur **"Select Files"** pour choisir votre image ou PDF
- Formats acceptés : Images (JPG, PNG, etc.) et PDF
- Le fichier sera sauvegardé dans `/static/depenseUpload/`

### Exemple de Réponse
```json
{
  "id": 1,
  "designation": "Achat de papeterie",
  "categorieId": 1,
  "categorieNom": "Fournitures de bureau",
  "prixUnitaire": 5000.0,
  "quantite": 10,
  "montant": 50000.0,
  "source": "CAISSE",
  "ordonnateur": "COMPTABLE",
  "numeroCheque": null,
  "typeCharge": "CHARGE_VARIABLE",
  "produitId": null,
  "produitNom": null,
  "fournisseurId": 1,
  "fournisseurNom": "Fournisseur ABC",
  "pieceJointe": null,
  "entrepriseId": 1,
  "entrepriseNom": "Mon Entreprise",
  "creeParId": 5,
  "creeParNom": "Jean Dupont",
  "creeParEmail": "jean.dupont@example.com",
  "dateCreation": "2025-01-15T14:30:00"
}
```

### Valeurs possibles pour les champs enum:

**source**: 
- `CAISSE`
- `BANQUE`
- `MOBILE_MONEY`
- `DETTE`

**ordonnateur**: 
- `MANAGER`
- `COMPTABLE`

**typeCharge**: 
- `CHARGE_FIXE`
- `CHARGE_VARIABLE`

### Note sur le champ `depense` dans Postman:

Le champ `depense` doit contenir un JSON valide avec tous les champs de la dépense. Voici un exemple complet formaté pour plus de lisibilité :

```json
{
  "designation": "Achat de papeterie",
  "categorieId": 1,
  "prixUnitaire": 5000.0,
  "quantite": 10,
  "source": "CAISSE",
  "ordonnateur": "COMPTABLE",
  "numeroCheque": null,
  "typeCharge": "CHARGE_VARIABLE",
  "produitId": null,
  "fournisseurId": 1,
  "pieceJointe": null
}
```

**Dans Postman, vous devez coller ce JSON (sur une seule ligne ou avec des sauts de ligne) dans le champ `depense` de type Text.**

---

## 4. Lister toutes les Dépenses Générales

### Endpoint
```
GET /api/auth/comptabilite/depenses-generales
```

### Headers
```
Authorization: Bearer {votre_token_jwt}
```

### Exemple de Réponse
```json
[
  {
    "id": 1,
    "designation": "Achat de papeterie",
    "categorieId": 1,
    "categorieNom": "Fournitures de bureau",
    "prixUnitaire": 5000.0,
    "quantite": 10,
    "montant": 50000.0,
    "source": "CAISSE",
    "ordonnateur": "COMPTABLE",
    "numeroCheque": null,
    "typeCharge": "CHARGE_VARIABLE",
    "produitId": null,
    "produitNom": null,
    "fournisseurId": 1,
    "fournisseurNom": "Fournisseur ABC",
    "pieceJointe": null,
    "entrepriseId": 1,
    "entrepriseNom": "Mon Entreprise",
    "creeParId": 5,
    "creeParNom": "Jean Dupont",
    "creeParEmail": "jean.dupont@example.com",
    "dateCreation": "2025-01-15T14:30:00"
  },
  {
    "id": 2,
    "designation": "Frais de transport",
    "categorieId": 2,
    "categorieNom": "Transport",
    "prixUnitaire": 2500.0,
    "quantite": 5,
    "montant": 12500.0,
    "source": "MOBILE_MONEY",
    "ordonnateur": "MANAGER",
    "numeroCheque": null,
    "typeCharge": "CHARGE_VARIABLE",
    "produitId": null,
    "produitNom": null,
    "fournisseurId": null,
    "fournisseurNom": null,
    "pieceJointe": "https://example.com/pieces/transport-001.pdf",
    "entrepriseId": 1,
    "entrepriseNom": "Mon Entreprise",
    "creeParId": 5,
    "creeParNom": "Jean Dupont",
    "creeParEmail": "jean.dupont@example.com",
    "dateCreation": "2025-01-15T15:00:00"
  }
]
```

---

## 5. Récupérer la Comptabilité (endpoint existant)

### Endpoint
```
GET /api/auth/comptabilite
```

### Headers
```
Authorization: Bearer {votre_token_jwt}
```

### Exemple de Réponse (partielle)
```json
{
  "chiffreAffaires": {
    "total": 1500000.0,
    "duJour": 45000.0,
    "duMois": 500000.0,
    "deLAnnee": 1500000.0,
    "totalVentes": 1200000.0,
    "totalFactures": 712500.0,
    "totalPaiementsFactures": 200000.0,
    "ventesDetails": [...],
    "factureDetails": [...],
    "nombreFacturesReelles": 5,
    "montantFacturesReelles": 712500.0,
    "nombreFacturesProforma": 2,
    "montantFacturesProforma": 150000.0
  },
  "ventes": {...},
  "facturation": {...},
  "depenses": {...},
  "boutiques": [...],
  "boutiquesDisponibles": [...],
  "clients": {...},
  "vendeurs": {...},
  "activites": {...}
}
```

---

## Gestion des Erreurs

### Erreur de Permission
```json
{
  "error": "Seul un comptable ou un utilisateur disposant de la permission COMPTABILITE peut créer une dépense générale."
}
```

### Erreur de Validation
```json
{
  "error": "La désignation est obligatoire."
}
```

### Erreur de Catégorie inexistante
```json
{
  "error": "Catégorie de dépense non trouvée."
}
```

### Erreur de Source invalide
```json
{
  "error": "Source invalide : INVALIDE. Valeurs acceptées : CAISSE, BANQUE, MOBILE_MONEY, DETTE"
}
```

---

## Notes Importantes

1. **Permissions**: Seuls les utilisateurs avec le rôle `COMPTABLE` ou la permission `COMPTABILITE` peuvent créer et consulter les dépenses générales.

2. **Format de la Requête**: L'endpoint utilise `multipart/form-data` pour permettre l'upload de fichiers. Dans Postman :
   - Utilisez l'onglet **"Body"** → **"form-data"**
   - Le champ `depense` doit être de type **"Text"** et contenir le JSON de la dépense
   - Le champ `pieceJointe` doit être de type **"File"** (facultatif)

3. **Calcul du Montant**: Le montant est calculé automatiquement : `montant = prixUnitaire × quantite`

4. **Catégories**: Vous pouvez soit :
   - Utiliser une catégorie existante en fournissant `categorieId`
   - Créer une nouvelle catégorie en fournissant `nouvelleCategorieNom` (sans `categorieId`)

5. **Champs Facultatifs**:
   - `categorieId` / `nouvelleCategorieNom` : Au moins l'un peut être fourni (ou aucun si vous ne voulez pas de catégorie)
   - `numeroCheque` : Facultatif
   - `produitId` : Facultatif (doit appartenir à l'entreprise)
   - `fournisseurId` : Facultatif (doit appartenir à l'entreprise)
   - `pieceJointe` : Facultatif (fichier image ou PDF)

6. **Upload de Fichiers**:
   - Le fichier sera sauvegardé dans `/static/depenseUpload/`
   - L'URL retournée sera de la forme `/depenseUpload/{uuid}_{nom_fichier}`
   - Formats acceptés : Images (JPG, PNG, GIF, etc.) et PDF
   - Taille maximale : Dépend de la configuration Spring Boot (par défaut 1MB)

7. **Vérifications**: Le système vérifie que les produits et fournisseurs appartiennent bien à l'entreprise de l'utilisateur.

8. **Ordre de Tri**: Les dépenses générales sont triées par date de création décroissante (plus récentes en premier).


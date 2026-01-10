# Exemple de Requête Postman - Envoi Facture Vente avec Pièces Jointes

## Endpoint
```
POST {{baseUrl}}/api/auth/factureVente/envoyer-email-avec-pieces-jointes
```

## Configuration Postman

### 1. Méthode et URL
- **Méthode** : `POST`
- **URL** : `http://localhost:8080/api/auth/factureVente/envoyer-email-avec-pieces-jointes`
  (ou votre URL de production : `https://xpertcash.tchakeda.com/api/v1/api/auth/factureVente/envoyer-email-avec-pieces-jointes`)

### 2. Headers
Assurez-vous d'inclure votre token d'authentification :
```
Authorization: Bearer <votre_token_jwt>
```

### 3. Body (form-data)
Dans Postman, allez dans l'onglet **Body** et sélectionnez **form-data**.

Ajoutez les champs suivants :

| Key | Type | Value | Description |
|-----|------|-------|-------------|
| `venteId` | Text | `1` | ID de la vente (requis) |
| `email` | Text | `client@example.com` | Email du destinataire (requis) |
| `attachments` | File | Sélectionnez un fichier PDF | Pièces jointes (optionnel, peut être plusieurs fichiers) |

### 4. Pour ajouter plusieurs fichiers
Si vous voulez envoyer plusieurs fichiers, ajoutez plusieurs champs avec la même clé `attachments` :
- Cliquez sur **Add another field**
- Nommez-le `attachments` (le même nom)
- Type : **File**
- Sélectionnez votre deuxième fichier

## Exemple Complet

### Configuration dans Postman

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: multipart/form-data (géré automatiquement par Postman)
```

**Body (form-data):**
```
venteId: 123
email: client@example.com
attachments: [Fichier: facture_123.pdf]
attachments: [Fichier: reçu_123.pdf]  (optionnel, pour plusieurs fichiers)
```

## Exemple avec cURL (pour référence)

```bash
curl --location 'http://localhost:8080/api/auth/factureVente/envoyer-email-avec-pieces-jointes' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'venteId="123"' \
--form 'email="client@example.com"' \
--form 'attachments=@"/path/to/facture.pdf"'
```

## Exemple avec plusieurs fichiers

```bash
curl --location 'http://localhost:8080/api/auth/factureVente/envoyer-email-avec-pieces-jointes' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'venteId="123"' \
--form 'email="client@example.com"' \
--form 'attachments=@"/path/to/facture.pdf"' \
--form 'attachments=@"/path/to/recu.pdf"'
```

## Exemple JavaScript (fetch API)

```javascript
const formData = new FormData();
formData.append('venteId', '123');
formData.append('email', 'client@example.com');

// Ajouter un fichier
const fileInput = document.querySelector('input[type="file"]');
formData.append('attachments', fileInput.files[0]);

// Pour plusieurs fichiers
for (let i = 0; i < fileInput.files.length; i++) {
    formData.append('attachments', fileInput.files[i]);
}

fetch('http://localhost:8080/api/auth/factureVente/envoyer-email-avec-pieces-jointes', {
    method: 'POST',
    headers: {
        'Authorization': 'Bearer YOUR_JWT_TOKEN'
        // Ne pas définir Content-Type, le navigateur le fera automatiquement
    },
    body: formData
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

## Réponse Succès (200 OK)
```json
"Facture envoyée par email avec succès"
```

## Réponse Erreur (400 Bad Request)
```json
"L'adresse email est requise"
```
ou
```json
"L'ID de la vente est requis"
```

## Réponse Erreur (500 Internal Server Error)
```json
"Erreur lors de l'envoi de l'email : [détails de l'erreur]"
```

## Notes Importantes

1. **Authentification** : Assurez-vous d'avoir un token JWT valide dans le header Authorization
2. **Format des fichiers** : Les fichiers PDF sont recommandés pour les factures
3. **Taille des fichiers** : Vérifiez les limites de taille configurées dans votre application (actuellement 50MB)
4. **Email valide** : L'email doit être au format valide, sinon le serveur SMTP rejettera la demande
5. **ID de vente** : L'ID doit correspondre à une vente existante dans la base de données


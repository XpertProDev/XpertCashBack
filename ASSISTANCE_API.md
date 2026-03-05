## Module d'assistance / support – Spécification API pour le frontend

Ce document décrit comment le frontend doit utiliser le module d'assistance (tickets + messages + pièces jointes).

### 1. Concepts

- **Ticket d'assistance** (`AssistanceTicket`) : une discussion entre un utilisateur et le support.
- **Message** (`AssistanceMessage`) : un message dans le ticket (texte + pièce jointe optionnelle).
- **Statut** (`AssistanceStatus`) :
  - `EN_ATTENTE` : le ticket vient d'être créé, le support n'a pas encore répondu.
  - `EN_COURS` : le support a commencé à traiter le ticket.
  - `RESOLU` : le support considère que le problème est résolu (en attente de validation client).
  - `CONTESTE` : le client a indiqué qu'il n'est pas d'accord (clic sur **Non** ou nouveau message après `RESOLU` non validé).
- **Pièce jointe** : capture d'écran ou document, stocké dans `supportUpload`, référencé par une URL (`/supportUpload/xxxx.png`).

Tous les endpoints sont protégés par l'authentification existante (JWT) et utilisent l'utilisateur connecté renvoyé par `AuthenticationHelper`.
**IMPORTANT : tous les chemins ci-dessous sont désormais préfixés par `/api/auth` (et non plus `/api/assistance`).**

---

### 2. Rôles et séparation des écrans

Il y a **deux profils** qui utilisent l’assistance :

- **Utilisateur normal** (ADMIN, MANAGER, UTILISATEUR, etc.)  
  - Utilise les routes **sans** `/admin` :
    - `POST /api/auth/tickets`
    - `GET /api/auth/tickets`
    - `GET /api/auth/tickets/{ticketId}/messages`
    - `POST /api/auth/tickets/{ticketId}/messages`
  - C’est **lui** qui ouvre les tickets vers le support.

- **Compte SUPPORT** (rôle `SUPPORT` ou `SUPER_ADMIN`)  
  - Utilise les routes **avec** `/admin` :
    - `GET /api/auth/admin/tickets`
    - `PATCH /api/auth/admin/tickets/{ticketId}/status`
    - `DELETE /api/auth/admin/tickets/{ticketId}`
    - `POST /api/auth/tickets/{ticketId}/messages` (même route que l’utilisateur pour répondre dans un ticket existant)
  - **Ne doit pas** appeler `POST /api/assistance/tickets` pour ouvrir un ticket, sinon le back renverra un 403 si on verrouille plus tard.

**Important pour les tests :**

- Si tu es connecté avec un **compte SUPPORT** et que tu appelles `POST /api/auth/tickets`, tu peux obtenir un **403 Forbidden** (ce endpoint est pensé pour le client final).  
- Pour tester la page “Assistance Client” côté utilisateur :
  - Connecte‑toi avec un **compte utilisateur classique** (ADMIN / MANAGER / UTILISATEUR rattaché à une entreprise, pas SUPPORT, pas SUPER_ADMIN).
  - Depuis ce compte, appelle `POST /api/auth/tickets` (avec le Bearer token de ce user).
- Pour le dashboard support (liste de tous les tickets, changement de statut, suppression) :
  - Connecte‑toi avec le **compte support** (`support@xpertcash.com`, rôle `SUPPORT`) ou un `SUPER_ADMIN`.
  - Utilise uniquement les routes `/api/auth/admin/...` pour lister/gérer les tickets.

---

### 3. Création d'un ticket (utilisateur)

**Endpoint**

- `POST /api/auth/tickets`
- `Content-Type: multipart/form-data`

**Champs du formulaire**

- `message` (string, requis) : description du problème.
- `pieceJointe` (file, optionnel) : capture ou document.

**Exemple (Postman / front)**

- Méthode : `POST`
- URL : `/api/auth/tickets`
- Body (form-data) :
  - key: `message`, type: text, value: `Depuis ce matin, j'ai une erreur "session expirée".`
  - key: `pieceJointe`, type: file, value: `capture.png` (optionnel)

**Réponse – `AssistanceTicketDTO` (JSON)**

```json
{
  "id": 1,
  "numeroTicket": "TCK-2026-AB12CD34",
  "sujet": "Depuis ce matin, j'ai une erreur \"session expirée\".",
  "statut": "EN_ATTENTE",
  "createdAt": "2026-03-03T10:15:30",
  "updatedAt": null,
  "closedAt": null,
  "deleted": false,
  "createdByNom": "Nom Utilisateur",
  "createdByEmail": "user@example.com",
  "messages": [
    {
      "id": 10,
      "ticketId": 1,
      "auteurId": 5,
      "auteurNom": "Nom Utilisateur",
      "contenu": "Depuis ce matin, j'ai une erreur \"session expirée\".",
      "pieceJointePath": "/supportUpload/uuid_capture.png",
      "support": false,
      "createdAt": "2026-03-03T10:15:30"
    }
  ]
}
```

---

### 4. Lister les tickets de l'utilisateur

**Endpoint**

- `GET /api/auth/tickets`

**Réponse**

Liste de `AssistanceTicketDTO` **sans** le détail des messages (liste courte pour l'écran de synthèse) :

```json
[
  {
    "id": 1,
    "numeroTicket": "TCK-2026-AB12CD34",
    "sujet": "Je n'arrive pas à me connecter",
    "statut": "EN_COURS",
    "createdAt": "2026-03-03T10:15:30",
    "updatedAt": "2026-03-03T10:20:00",
    "closedAt": null,
    "deleted": false,
    "createdByNom": "Nom Utilisateur",
    "createdByEmail": "user@example.com",
    "messages": []
  }
]
```

Le backend génère automatiquement `sujet` à partir du premier message (les ~80 premiers caractères).
Le frontend peut se baser sur :

- `sujet` (auto-généré) pour le titre de la carte.
- `statut` pour afficher les badges (`En attente`, `En cours`, `Résolu`).
- `createdAt` / `updatedAt` pour les dates visibles dans la carte.

---

### 5. Récupérer tous les messages d'un ticket

**Endpoint**

- `GET /api/auth/tickets/{ticketId}/messages`

**Réponse – liste de `AssistanceMessageDTO`**

```json
[
  {
    "id": 10,
    "ticketId": 1,
    "auteurId": 5,
    "auteurNom": "Nom Utilisateur",
    "contenu": "Depuis ce matin, j'ai une erreur \"session expirée\".",
    "pieceJointePath": "/supportUpload/uuid_capture.png",
    "support": false,
    "createdAt": "2026-03-03T10:15:30"
  },
  {
    "id": 11,
    "ticketId": 1,
    "auteurId": 2,
    "auteurNom": "Support XpertCash",
    "contenu": "Bonjour, pouvez-vous réessayer après avoir vidé le cache ?",
    "pieceJointePath": null,
    "support": true,
    "createdAt": "2026-03-03T10:20:00"
  }
]
```

- `support = true` : message du support (bulle couleur différente côté UI).
- `pieceJointePath` : URL à utiliser dans une balise `<img>` ou lien de téléchargement.

---

### 6. Ajouter un message dans un ticket (utilisateur OU support)

**Endpoint**

- `POST /api/auth/tickets/{ticketId}/messages`
- `Content-Type: multipart/form-data`

**Champs**

- `message` (string, requis)
- `pieceJointe` (file, optionnel)

**Réponse**

Un `AssistanceMessageDTO` (même format que ci-dessus).

**Règles métier**

- Si c'est un **utilisateur normal** : il peut envoyer des messages seulement sur **SES** tickets.
- Si c'est un **utilisateur SUPPORT** :
  - Il peut répondre aux tickets de son entreprise support.
  - Si le ticket est en `EN_ATTENTE` ou `CONTESTE` et que le support répond, le statut passe automatiquement à `EN_COURS`.
- Si le ticket est en `RESOLU` :
  - Tant que le client **n'a pas validé**, il peut :
    - soit cliquer sur **Non** (voir endpoint de refus ci‑dessous),
    - soit renvoyer un nouveau message, ce qui met le ticket en statut `CONTESTE`.
  - Si le client a **validé** (`valider`), toute nouvelle tentative de message renvoie une erreur métier :
    `"Ce ticket est déjà résolu et validé. Merci de créer un nouveau ticket pour un autre problème."`

---

### 7. Vue Support (rôle SUPPORT ou SUPER_ADMIN)

Le compte support identifié dans le backend a le rôle `SUPPORT` (créé dans `SuperAdminInitializer`).

**Récap : ce que le support peut faire**

| Action | Endpoint | Description |
|--------|----------|-------------|
| Voir le rapport / statistiques | `GET /api/auth/admin/rapport` | Chiffres pour le dashboard (total, en attente, en cours, résolus validés, nouveaux 7j/30j). |
| Lister tous les tickets | `GET /api/auth/admin/tickets` | Liste (optionnel : filtre par `status`). |
| Voir les messages d’un ticket | `GET /api/auth/tickets/{ticketId}/messages` | Même route que le client. |
| Répondre dans un ticket | `POST /api/auth/tickets/{ticketId}/messages` | Même route que le client (multipart : `message`, `pieceJointe`). |
| Changer le statut (ex. Résolu) | `PATCH /api/auth/admin/tickets/{ticketId}/status` | Body : `{ "status": "RESOLU" }`. |
| Supprimer un ticket | `DELETE /api/auth/admin/tickets/{ticketId}` | Suppression définitive (ticket + messages + pièces jointes). |

#### 7.0. Rapport / statistiques (dashboard support)

**Endpoint**

- `GET /api/auth/admin/rapport`

**Réponse (JSON)**

```json
{
  "totalTickets": 42,
  "enAttente": 5,
  "enCours": 8,
  "contestes": 3,
  "resolusEnAttenteValidation": 12,
  "resolusValides": 17,
  "nouveauxCetteSemaine": 6,
  "nouveauxCeMois": 22
}
```

- `totalTickets` : nombre total de tickets non supprimés.
- `enAttente` : statut EN_ATTENTE.
- `enCours` : statut EN_COURS.
- `contestes` : statut CONTESTE (tickets où le client n’est pas d’accord).
- `resolusEnAttenteValidation` : statut RESOLU, client n’a pas encore validé.
- `resolusValides` : statut RESOLU et client a validé.
- `nouveauxCetteSemaine` / `nouveauxCeMois` : tickets créés sur les 7 ou 30 derniers jours.

Le front peut utiliser ces données pour des cartes ou graphiques sur la page dashboard support.

#### 7.1. Rapport historique (par jour)

**Endpoint**

- `GET /api/auth/admin/rapport/historique?periode=SEMAINE|MOIS`

**Paramètres**

- `periode` (query param, optionnel) :
  - `SEMAINE` (par défaut) : les 7 derniers jours.
  - `MOIS` : les 30 derniers jours.

**Réponse (JSON)**

Tableau de lignes agrégées **par jour** :

```json
[
  { "date": "2026-03-01", "nouveaux": 3, "enCours": 5, "resolus": 2, "contestes": 1 },
  { "date": "2026-03-02", "nouveaux": 1, "enCours": 4, "resolus": 3, "contestes": 0 }
]
```

- `date` : jour au format `YYYY-MM-DD`.
- `nouveaux` : nombre de tickets créés ce jour-là.
- `enCours` : nombre de tickets passés en `EN_COURS` ce jour-là.
- `resolus` : nombre de tickets résolus (statut `RESOLU`) ce jour-là.
- `contestes` : nombre de tickets passés en `CONTESTE` ce jour-là (client non satisfait).

Le frontend peut utiliser ce tableau pour tracer un **graphique en courbe** ou **barres empilées** sur le temps.

#### 7.2. Lister les tickets à traiter

**Endpoint**

- `GET /api/auth/admin/tickets`
- Option de filtrage par statut via body JSON (ou tu peux adapter en query param côté front si tu préfères).

Exemple body (optionnel) :

```json
{ "status": "EN_ATTENTE" }
```

**Réponse**

Liste de `AssistanceTicketDTO` (comme pour l'utilisateur, mais pour tous les tickets de l'entreprise support).

#### 7.3. Changer le statut d'un ticket

**Endpoint**

- `PATCH /api/auth/admin/tickets/{ticketId}/status`

**Body JSON**

```json
{ "status": "RESOLU" }
```

Statuts possibles : `EN_ATTENTE`, `EN_COURS`, `RESOLU`.

#### 7.4. Supprimer un ticket

**Endpoint**

- `DELETE /api/auth/admin/tickets/{ticketId}`

**Effets côté backend**

- Suppression **définitive** du ticket et de tous ses messages en base (CASCADE).
- Suppression physique des pièces jointes associées dans `supportUpload`.

---

### 8. Validation / refus côté client

Sur un ticket en statut `RESOLU`, le client a deux actions possibles :

- **Valider** (le problème est bien résolu) :
  - Endpoint : `PATCH /api/auth/tickets/{ticketId}/valider`
  - Effet : `valideParClient = true`, le ticket devient **fermement clôturé**.
  - Toute nouvelle tentative de message renvoie une erreur métier.

- **Refuser** (il n’est pas satisfait, clic sur **Non**) :
  - Endpoint : `PATCH /api/auth/tickets/{ticketId}/refuser`
  - Effet : le statut passe à `CONTESTE`, `valideParClient = false`.
  - Le support voit clairement que le ticket est **contesté** et peut reprendre en main le dossier (une réponse du support remettra le ticket en `EN_COURS`).

---

### 9. Upload des pièces jointes – Rappel

Pas de route d'upload séparée : l'upload est intégré dans les mêmes requêtes que :

- **Création de ticket** (`POST /api/auth/tickets`, multipart/form-data).
- **Ajout d'un message** (`POST /api/auth/tickets/{ticketId}/messages`, multipart/form-data).

Le backend :

- Sauvegarde le fichier dans `src/main/resources/static/supportUpload`.
- Retourne une URL publique de type `"/supportUpload/{nomFichier}"` dans `pieceJointePath`.

---

### 10. Mapping UI ↔ API (comme l'image fournie)

Pour chaque carte de ticket dans l'écran Assistance :

- **Titre** : `sujet`
- **Sous-titre** : premier ou dernier message (à toi de choisir) – récupérable via `/tickets/{id}/messages`.
- **Date affichée** :
  - `updatedAt` si non nul, sinon `createdAt`.
- **Statut** :
  - `EN_ATTENTE` → badge jaune "En attente"
  - `EN_COURS` → badge bleu "En cours"
  - `RESOLU` → badge vert "Résolu"
- **Numéro de ticket** : `numeroTicket` (affichable dans un coin de la carte ou détail).

Pour la vue détail (chat) :

- Utiliser `/api/auth/tickets/{ticketId}/messages`.
- Bulle alignée à droite + couleur support si `support = true`.
- Bulle alignée à gauche + couleur classique sinon.
- Si `pieceJointePath` non nul, afficher l'image ou un lien sous le message.


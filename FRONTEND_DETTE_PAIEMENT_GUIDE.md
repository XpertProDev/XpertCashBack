# Guide Front-End : Affichage des Dettes et Paiements

## 📋 Vue d'ensemble

Le système permet maintenant de **lier clairement les dettes à leurs paiements**. Voici comment le front-end doit utiliser ces données pour créer une interface utilisateur intuitive.

---

## 🔍 1. Identification des Types de Transactions

### Dans `/api/auth/comptabilite/complete`

Chaque transaction a maintenant ces champs clés :

```typescript
interface Transaction {
  id: number;
  numero: string;
  typeTransaction: "ENTREE" | "SORTIE" | "DETTE";
  origine: "COMPTABILITE" | "PAIEMENT_DETTE" | "FACTURE" | "BOUTIQUE";
  
  // 🔗 Champs pour les dettes et paiements
  detteId: number | null;        // ID de la dette payée (null si c'est la dette elle-même)
  detteType: "VENTE_CREDIT" | "ENTREE_DETTE" | null;
  detteNumero: string | null;    // Numéro de référence de la dette
}
```

### Règles d'identification :

| Condition | Type | Origine | Signification |
|-----------|------|---------|---------------|
| `source === "DETTE"` | `"DETTE"` | `"COMPTABILITE"` | **Dette non payée** (créance) |
| `origine === "PAIEMENT_DETTE"` | `"ENTREE"` | `"PAIEMENT_DETTE"` | **Paiement d'une dette** |
| `origine === "COMPTABILITE"` + pas de `detteId` | `"ENTREE"` | `"COMPTABILITE"` | **Entrée classique** (vente cash, autre) |

---

## 🎨 2. Affichage Visuel Recommandé

### A. Liste des Transactions (`/comptabilite/complete`)

#### Pour une **Dette** (`typeTransaction: "DETTE"`) :

```tsx
<TransactionCard>
  <Badge color="warning">DETTE</Badge>
  <Badge color="info">{detteType}</Badge> {/* "ENTREE_DETTE" ou "VENTE_CREDIT" */}
  
  <Title>{designation}</Title>
  <Info>
    <span>Numéro: {numero}</span>
    <span>Montant: {montant} FCFA</span>
    <span>Reste à payer: {montantReste} FCFA</span> {/* Si disponible */}
  </Info>
  
  {/* Optionnel: Afficher les paiements liés */}
  <Button onClick={() => showPaiements(detteId)}>
    Voir les paiements
  </Button>
</TransactionCard>
```

#### Pour un **Paiement de Dette** (`origine: "PAIEMENT_DETTE"`) :

```tsx
<TransactionCard>
  <Badge color="success">ENTREE</Badge>
  <Badge color="primary">PAIEMENT DETTE</Badge>
  
  <Title>{designation}</Title>
  <Info>
    <span>Numéro paiement: {numero}</span>
    <span>Montant payé: {montant} FCFA</span>
    
    {/* 🔗 Lien vers la dette */}
    <Link to={`/dettes/${detteId}`}>
      Dette #{detteNumero} ({detteType})
    </Link>
  </Info>
  
  <Icon name="link" /> {/* Icône pour indiquer le lien */}
</TransactionCard>
```

#### Pour une **Entrée Classique** :

```tsx
<TransactionCard>
  <Badge color="success">ENTREE</Badge>
  
  <Title>{designation}</Title>
  <Info>
    <span>Numéro: {numero}</span>
    <span>Montant: {montant} FCFA</span>
  </Info>
</TransactionCard>
```

---

## 📊 3. Vue Détail d'une Dette

### Endpoint : `/api/auth/tresorerie/dettes`

```typescript
interface DetteItem {
  id: number;
  type: "VENTE_CREDIT" | "ENTREE_DETTE" | "FACTURE_IMPAYEE" | "DEPENSE_DETTE";
  montantInitial: number;
  montantRestant: number;
  numero: string;
  date: string;
  client?: string;
  responsable?: string;
}
```

### Affichage recommandé :

```tsx
<DetteDetail>
  <Header>
    <Title>Dette #{numero}</Title>
    <Badge>{type}</Badge>
  </Header>
  
  <Stats>
    <StatCard>
      <Label>Montant initial</Label>
      <Value>{montantInitial} FCFA</Value>
    </StatCard>
    
    <StatCard>
      <Label>Montant payé</Label>
      <Value>{montantInitial - montantRestant} FCFA</Value>
      <ProgressBar 
        value={(montantInitial - montantRestant) / montantInitial * 100} 
      />
    </StatCard>
    
    <StatCard highlight>
      <Label>Reste à payer</Label>
      <Value>{montantRestant} FCFA</Value>
    </StatCard>
  </Stats>
  
  {/* Liste des paiements */}
  <Section>
    <Title>Historique des paiements</Title>
    {paiements.map(paiement => (
      <PaiementItem>
        <Date>{paiement.dateCreation}</Date>
        <Amount>{paiement.montant} FCFA</Amount>
        <Mode>{paiement.modeEntree}</Mode>
      </PaiementItem>
    ))}
  </Section>
</DetteDetail>
```

---

## 🔗 4. Fonctionnalités à Implémenter

### A. Filtrage dans la Comptabilité

```tsx
<Filters>
  <Select 
    label="Type de transaction"
    options={[
      { value: "all", label: "Toutes" },
      { value: "DETTE", label: "Dettes" },
      { value: "ENTREE", label: "Entrées" },
      { value: "SORTIE", label: "Sorties" }
    ]}
    onChange={filterByType}
  />
  
  <Select 
    label="Origine"
    options={[
      { value: "all", label: "Toutes" },
      { value: "PAIEMENT_DETTE", label: "Paiements de dettes" },
      { value: "COMPTABILITE", label: "Entrées classiques" }
    ]}
    onChange={filterByOrigine}
  />
</Filters>
```

### B. Lien Clicable Dette ↔ Paiement

```tsx
// Dans la liste des transactions
{transaction.origine === "PAIEMENT_DETTE" && (
  <Link 
    to={`/comptabilite/dettes/${transaction.detteId}`}
    className="dette-link"
  >
    <Icon name="link" />
    Voir la dette #{transaction.detteNumero}
  </Link>
)}

// Dans le détail d'une dette
<Button onClick={() => showPaiements(transaction.id)}>
  Voir les paiements ({paiementsCount})
</Button>
```

### C. Regrouper les Paiements par Dette

```tsx
// Grouper les paiements par detteId
const paiementsParDette = transactions
  .filter(t => t.origine === "PAIEMENT_DETTE")
  .reduce((acc, paiement) => {
    const detteId = paiement.detteId;
    if (!acc[detteId]) acc[detteId] = [];
    acc[detteId].push(paiement);
    return acc;
  }, {});

// Afficher
{Object.entries(paiementsParDette).map(([detteId, paiements]) => (
  <DetteGroup>
    <DetteHeader>
      Dette #{paiements[0].detteNumero}
      <Total>{sum(paiements.map(p => p.montant))} FCFA payés</Total>
    </DetteHeader>
    {paiements.map(paiement => (
      <PaiementItem paiement={paiement} />
    ))}
  </DetteGroup>
))}
```

---

## 🎯 5. Exemples de Requêtes API

### Récupérer toutes les transactions avec filtres

```typescript
// GET /api/auth/comptabilite/complete?page=0&size=20
const transactions = await fetch('/api/auth/comptabilite/complete?page=0&size=20');

// Filtrer côté front-end
const dettes = transactions.filter(t => t.typeTransaction === "DETTE");
const paiementsDette = transactions.filter(t => t.origine === "PAIEMENT_DETTE");
const entreesClassiques = transactions.filter(
  t => t.typeTransaction === "ENTREE" && t.origine === "COMPTABILITE"
);
```

### Récupérer les dettes détaillées

```typescript
// GET /api/auth/tresorerie/dettes?page=0&size=20
const dettes = await fetch('/api/auth/tresorerie/dettes?page=0&size=20');
```

### Payer une dette

```typescript
// POST /api/auth/comptabilite/dettes/payer
const response = await fetch('/api/auth/comptabilite/dettes/payer', {
  method: 'POST',
  body: JSON.stringify({
    detteId: 3,
    type: "ENTREE_DETTE", // ou "VENTE_CREDIT"
    montant: 40000,
    modePaiement: "ESPECES" // ou "VIREMENT", "MOBILE_MONEY", etc.
  })
});
```

---

## 📱 6. Indicateurs Visuels Recommandés

### Couleurs et Icônes :

| Type | Couleur | Icône | Badge |
|------|---------|-------|-------|
| Dette non payée | 🟡 Orange/Warning | ⚠️ | `DETTE` |
| Paiement de dette | 🔵 Bleu/Primary | 🔗 | `PAIEMENT DETTE` |
| Entrée classique | 🟢 Vert/Success | ✅ | `ENTREE` |

### Barre de progression pour les dettes :

```tsx
<ProgressBar 
  value={(montantInitial - montantRestant) / montantInitial * 100}
  color={montantRestant === 0 ? "success" : "warning"}
/>
```

---

## ✅ 7. Checklist Front-End

- [ ] Afficher un badge distinct pour les paiements de dettes (`origine: "PAIEMENT_DETTE"`)
- [ ] Afficher un badge distinct pour les dettes (`typeTransaction: "DETTE"`)
- [ ] Rendre cliquable le lien vers la dette depuis un paiement (`detteId` + `detteNumero`)
- [ ] Afficher le montant initial et le montant restant pour chaque dette
- [ ] Grouper visuellement les paiements par dette (optionnel mais recommandé)
- [ ] Ajouter des filtres pour séparer les paiements de dettes des entrées classiques
- [ ] Afficher une barre de progression pour le pourcentage payé
- [ ] Permettre de payer une dette depuis l'interface (bouton "Payer")
- [ ] Afficher l'historique des paiements pour chaque dette
- [ ] Calculer et afficher le total payé par dette

---

## 🚀 8. Exemple de Composant React Complet

```tsx
import React from 'react';

interface Transaction {
  id: number;
  numero: string;
  designation: string;
  montant: number;
  typeTransaction: "ENTREE" | "SORTIE" | "DETTE";
  origine: string;
  detteId?: number | null;
  detteType?: string | null;
  detteNumero?: string | null;
}

const TransactionCard: React.FC<{ transaction: Transaction }> = ({ transaction }) => {
  const isDette = transaction.typeTransaction === "DETTE";
  const isPaiementDette = transaction.origine === "PAIEMENT_DETTE";
  
  return (
    <div className={`transaction-card ${isDette ? 'dette' : isPaiementDette ? 'paiement-dette' : 'classique'}`}>
      {/* Badges */}
      <div className="badges">
        {isDette && (
          <span className="badge badge-warning">
            DETTE {transaction.detteType && `(${transaction.detteType})`}
          </span>
        )}
        {isPaiementDette && (
          <span className="badge badge-primary">
            PAIEMENT DETTE
          </span>
        )}
        {!isDette && !isPaiementDette && (
          <span className="badge badge-success">ENTREE</span>
        )}
      </div>
      
      {/* Contenu */}
      <h3>{transaction.designation}</h3>
      <p>Numéro: {transaction.numero}</p>
      <p className="montant">{transaction.montant.toLocaleString()} FCFA</p>
      
      {/* Lien vers la dette si c'est un paiement */}
      {isPaiementDette && transaction.detteId && (
        <a 
          href={`/dettes/${transaction.detteId}`}
          className="dette-link"
        >
          🔗 Voir la dette #{transaction.detteNumero}
        </a>
      )}
    </div>
  );
};

export default TransactionCard;
```

---

## 📝 Résumé

Le front-end doit :

1. **Identifier visuellement** les 3 types de transactions :
   - Dettes (`typeTransaction: "DETTE"`)
   - Paiements de dettes (`origine: "PAIEMENT_DETTE"`)
   - Entrées classiques (le reste)

2. **Créer des liens** entre paiements et dettes via `detteId` et `detteNumero`

3. **Afficher les montants** : initial, payé, restant

4. **Permettre le filtrage** pour séparer les paiements de dettes des entrées classiques

5. **Grouper visuellement** les paiements par dette (optionnel mais recommandé)

C'est tout ! 🎉


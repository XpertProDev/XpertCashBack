-- Migration V1_18: Corriger remise et taux_remise (arrondi à 2 décimales)
-- 1. Remise: corrige les erreurs de précision (ex: 99999.99999999999 → 100000)
-- 2. Taux: recalculé à partir de la remise arrondie pour cohérence
-- Ex: remise 100000, totalht 3300000 → taux 3.03, pièce jointe affiche "Remise (3.03%)"

UPDATE facture_pro_forma
SET remise = ROUND(remise, 2),
    taux_remise = ROUND(ROUND(remise, 2) / totalht * 100, 2)
WHERE remise IS NOT NULL AND totalht > 0;

UPDATE facture_reelle
SET remise = ROUND(remise, 2),
    taux_remise = ROUND(ROUND(remise, 2) / totalht * 100, 2)
WHERE remise IS NOT NULL AND totalht > 0;

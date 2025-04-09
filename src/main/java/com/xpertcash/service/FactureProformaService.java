package com.xpertcash.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Client;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.LigneFactureProforma;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.StatutFactureProForma;
import com.xpertcash.entity.StatutPaiementFacture;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.ProduitRepository;

import jakarta.transaction.Transactional;

@Service
public class FactureProformaService {

    @Autowired
    private FactureProformaRepository factureProformaRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;
    
    public FactureProForma ajouterFacture(FactureProForma facture, Double remisePourcentage, Boolean appliquerTVA) {
        if (facture == null) {
            throw new RuntimeException("La facture ne peut pas être vide !");
        }
    
        // Vérifier la présence d'un client ou entreprise
        if ((facture.getClient() == null || facture.getClient().getId() == null) &&
            (facture.getEntrepriseClient() == null || facture.getEntrepriseClient().getId() == null)) {
            throw new RuntimeException("Un client ou une entreprise doit être spécifié pour la facture !");
        }

         // Génération de numéro de la facture automatiquement
        facture.setNumeroFacture(generateNumeroFacture());
    
        // Vérifier que la remise est comprise entre 0 et 100%
        if (remisePourcentage == null) {
            remisePourcentage = 0.0;
        } else if (remisePourcentage < 0 || remisePourcentage > 100) {
            throw new RuntimeException("Le pourcentage de remise doit être compris entre 0 et 100 !");
        }
        
    
        Long clientId = (facture.getClient() != null) ? facture.getClient().getId() : null;
        Long entrepriseClientId = (facture.getEntrepriseClient() != null) ? facture.getEntrepriseClient().getId() : null;
    
        // Vérifier si une facture similaire existe déjà
        List<FactureProForma> facturesExistantes = factureProformaRepository.findExistingFactures(clientId, entrepriseClientId, StatutFactureProForma.BROUILLON);
    
        for (FactureProForma fExistante : facturesExistantes) {
            List<Long> produitsExistants = fExistante.getLignesFacture()
                                                    .stream()
                                                    .map(l -> l.getProduit().getId())
                                                    .collect(Collectors.toList());
    
            List<Long> nouveauxProduits = facture.getLignesFacture()
                                                 .stream()
                                                 .map(l -> l.getProduit().getId())
                                                 .collect(Collectors.toList());
    
            if (new HashSet<>(produitsExistants).equals(new HashSet<>(nouveauxProduits))) {
                throw new RuntimeException("Une facture avec ces produits existe déjà pour ce client ou cette entreprise !");
            }
        }
    
        // Associer Client ou Entreprise
        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client introuvable !"));
            facture.setClient(client);
        }
    
        if (entrepriseClientId != null) {
            EntrepriseClient entrepriseClient = entrepriseClientRepository.findById(entrepriseClientId)
                    .orElseThrow(() -> new RuntimeException("Entreprise introuvable !"));
            facture.setEntrepriseClient(entrepriseClient);
        }
    
        // Initialisation des valeurs
        facture.setStatut(StatutFactureProForma.BROUILLON);
        facture.setDateCreation(LocalDate.now());
    
        double montantTotalHT = 0;
        if (facture.getLignesFacture() != null) {
            for (LigneFactureProforma ligne : facture.getLignesFacture()) {
                Produit produit = produitRepository.findById(ligne.getProduit().getId())
                        .orElseThrow(() -> new RuntimeException("Produit avec ID " + ligne.getProduit().getId() + " introuvable !"));
    
                ligne.setFactureProForma(facture);
                ligne.setProduit(produit);
                ligne.setPrixUnitaire(produit.getPrixVente());

                ligne.setMontantTotal(ligne.getQuantite() * ligne.getPrixUnitaire());
    
                // Ajout au montant total HT
                montantTotalHT += ligne.getMontantTotal();
            }
        }
    
        // Calcul de la remise
        double remiseMontant = (remisePourcentage > 0) ? montantTotalHT * (remisePourcentage / 100) : 0;
    
        // Appliquer la TVA uniquement si elle est activée
        boolean tvaActive = (appliquerTVA != null && appliquerTVA) || facture.isTva();
        double montantTVA = tvaActive ? (montantTotalHT - remiseMontant) * 0.18 : 0;
    
        // Calcul du montant total à payer
        double montantTotalAPayer = (montantTotalHT - remiseMontant) + montantTVA;
    
        // Assigner les montants calculés à la facture
        facture.setTotalHT(montantTotalHT);
        facture.setRemise(remiseMontant);  // Remise en montant
        facture.setTva(tvaActive);
        facture.setTotalFacture(montantTotalAPayer);
    
        return factureProformaRepository.save(facture);
    }
    
    
    // Méthode pour générer un numéro de facture unique
        private String generateNumeroFacture() {
            // Récupérer la date actuelle
            LocalDate currentDate = LocalDate.now();
            String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));

            // Récupérer l'index de la dernière facture pour cette date
            Optional<FactureProForma> lastFacture = factureProformaRepository.findTopByDateCreationOrderByNumeroFactureDesc(currentDate);

            // Extraire l'index de la dernière facture (s'il existe)
            int newIndex = 1;
            if (lastFacture.isPresent()) {
                String lastNumeroFacture = lastFacture.get().getNumeroFacture();
                // Assumer que le format est "FACTURE PROFORMA N°XXX-dd-MM-yyyy"
                String[] parts = lastNumeroFacture.split("-");
                newIndex = Integer.parseInt(parts[0].replace("FACTURE PROFORMA N°", "")) + 1;
            }

            // Créer le numéro de la nouvelle facture
            return String.format("FACTURE PROFORMA N°%03d-%s", newIndex, formattedDate);
        }


    public FactureProForma changerStatut(Long factureId, StatutFactureProForma nouveauStatut) {
        FactureProForma facture = factureProformaRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée"));

        if (!estTransitionValide(facture.getStatut(), nouveauStatut)) {
            throw new RuntimeException("Changement de statut invalide !");
        }

        facture.setStatut(nouveauStatut);
        return factureProformaRepository.save(facture); 
    }

    private boolean estTransitionValide(StatutFactureProForma actuel, StatutFactureProForma nouveau) {
        Map<StatutFactureProForma, List<StatutFactureProForma>> transitions = new HashMap<>();
        transitions.put(StatutFactureProForma.BROUILLON, List.of(StatutFactureProForma.APPROUVE));
        transitions.put(StatutFactureProForma.APPROUVE, List.of(StatutFactureProForma.ENVOYE));
        transitions.put(StatutFactureProForma.ENVOYE, List.of(StatutFactureProForma.VALIDE));

        return transitions.getOrDefault(actuel, List.of()).contains(nouveau);
    }


    // Méthode pour modifier une facture pro forma
    @Transactional
    public FactureProForma modifierFacture(Long factureId, Double remisePourcentage, Boolean appliquerTVA, FactureProForma modifications) {
        FactureProForma facture = factureProformaRepository.findById(factureId)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée !"));
    
        // Ajouter un log pour voir ce que vous recevez comme modifications
        System.out.println("Modifications reçues: " + modifications);
    
        // Vérifier si la facture est ENCAISSÉE
        if (facture.getStatutPaiement() == StatutPaiementFacture.ENCAISSE) {
            throw new RuntimeException("Impossible de modifier une facture dont le statut de paiement est ENCAISSÉ !");
        }
    
        // Vérifier si la facture est VALIDÉE
        if (facture.getStatut() == StatutFactureProForma.VALIDE) {
            // Si seule la modification du statut de paiement est demandée
            if (modifications.getStatutPaiement() != null) {
                System.out.println("Modification du statut de paiement: " + modifications.getStatutPaiement());
                // Appliquer le nouveau statut de paiement
                facture.setStatutPaiement(modifications.getStatutPaiement());
                return factureProformaRepository.save(facture);
            }
    
            // Si une tentative de modification autre que le statut de paiement est effectuée, on lève une exception
            if (modifications.getLignesFacture() != null || 
                remisePourcentage != null || 
                appliquerTVA != null || 
                modifications.getStatut() != null) {
                
                throw new RuntimeException("Impossible de modifier une facture VALIDÉE, sauf son statut de paiement !");
            }
    
            return facture; // Retourner la facture sans aucune modification si c'est une facture VALIDÉE
        }
    
        // Si la facture n'est pas VALIDÉE, on applique les autres modifications
        if (modifications.getLignesFacture() != null) {
            facture.getLignesFacture().clear();
            for (LigneFactureProforma ligne : modifications.getLignesFacture()) {
                Produit produit = produitRepository.findById(ligne.getProduit().getId())
                        .orElseThrow(() -> new RuntimeException("Produit introuvable !"));
                ligne.setFactureProForma(facture);
                ligne.setProduit(produit);
                ligne.setMontantTotal(ligne.getQuantite() * produit.getPrixVente());
                facture.getLignesFacture().add(ligne);
            }
        }
    
        // Vérifier et appliquer la remise
        remisePourcentage = (remisePourcentage == null) ? 0.0 : remisePourcentage;
        if (remisePourcentage < 0 || remisePourcentage > 100) {
            throw new RuntimeException("Le pourcentage de remise doit être compris entre 0 et 100 !");
        }
    
        // Calcul du montant total HT
        double montantTotalHT = facture.getLignesFacture().stream()
                .mapToDouble(LigneFactureProforma::getMontantTotal)
                .sum();
    
        double remiseMontant = montantTotalHT * (remisePourcentage / 100);
        boolean tvaActive = (appliquerTVA != null && appliquerTVA);
        double montantTVA = tvaActive ? (montantTotalHT - remiseMontant) * 0.18 : 0;
        double montantTotalAPayer = (montantTotalHT - remiseMontant) + montantTVA;
    
        // Mise à jour des valeurs calculées
        facture.setTotalHT(montantTotalHT);
        facture.setRemise(remiseMontant);
        facture.setTva(tvaActive);
        facture.setTotalFacture(montantTotalAPayer);
    
        // Mise à jour du statut si fourni (sauf si la facture est VALIDÉE ou déjà ENCAISSÉ)
        if (modifications.getStatut() != null && facture.getStatut() != StatutFactureProForma.VALIDE) {
            facture.setStatut(modifications.getStatut());
        }
    
        return factureProformaRepository.save(facture);
    }
    
}
 
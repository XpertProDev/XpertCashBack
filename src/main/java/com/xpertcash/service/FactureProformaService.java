package com.xpertcash.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Client;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.LigneFactureProforma;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.StatutFactureProForma;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.ProduitRepository;

@Service
public class FactureProformaService {

    @Autowired
    private FactureProformaRepository factureProformaRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private ClientRepository clientRepository;
    
    public FactureProForma ajouterFacture(FactureProForma facture) {
        if (facture == null) {
            throw new RuntimeException("La facture ne peut pas être vide !");
        }
    
        // Vérifier si le client existe dans la base de données
        if (facture.getClient() == null || facture.getClient().getId() == null) {
            throw new RuntimeException("Le client doit être spécifié pour la facture !");
        }
    
        // Récupérer la facture avec le client (en utilisant une requête JOIN FETCH)
        Client client = clientRepository.findById(facture.getClient().getId())
                .orElseThrow(() -> new RuntimeException("Client introuvable !"));
    
        // Assigner le statut par défaut et la date de création
        facture.setStatut(StatutFactureProForma.BROUILLON);
        facture.setDateCreation(LocalDate.now());
    
        // Assigner le client récupéré à la facture
        facture.setClient(client);
    
        // Gérer les lignes de facture
        if (facture.getLignesFacture() != null) {
            for (LigneFactureProforma ligne : facture.getLignesFacture()) {
                // 🔥 Vérifie si le produit existe
                Produit produit = produitRepository.findById(ligne.getProduit().getId())
                        .orElseThrow(() -> new RuntimeException("Produit avec ID " + ligne.getProduit().getId() + " introuvable !"));
    
                // Associe la ligne à la facture
                ligne.setFactureProForma(facture);
                ligne.setProduit(produit);
            }
        }
    
        // Sauvegarder la facture avec le client et les lignes
        return factureProformaRepository.save(facture);
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
}

package com.xpertcash.service;

import com.xpertcash.entity.*;
import com.xpertcash.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TransfertProduitService {

    @Autowired
    private ProduitsRepository produitsRepository;

    @Autowired
    private MagasinRepository magasinRepository;

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private ProduitBoutiqueRepository produitBoutiqueRepository;
    @Autowired
    private TransfertProduitRepository transfertProduitRepository;
    @Autowired
    private TransfertHistoriqueRepository transfertHistoriqueRepository;

    // Méthode pour effectuer le transfert
    public void transfererProduit(Long magasinId, Long boutiqueId, Long produitId, int quantite) {
        // Récupérer les objets nécessaires
        Magasin magasin = magasinRepository.findById(magasinId)
                .orElseThrow(() -> new RuntimeException("Magasin non trouvé"));
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));
        Produits produit = produitsRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        // Vérifier si la quantité dans le magasin est suffisante
        if (produit.getQuantite() < quantite) {
            throw new RuntimeException("Quantité insuffisante dans le magasin");
        }

        // Enregistrer l'ancienne quantité
        int ancienneQuantite = produit.getQuantite();

        // Mettre à jour la quantité du produit dans le magasin
        produit.setQuantite(ancienneQuantite - quantite);
        produitsRepository.save(produit);

        // Ajouter la quantité au produit dans la boutique
        Optional<ProduitBoutique> produitBoutiqueOpt = boutique.getProduitsBoutique().stream()
                .filter(pb -> pb.getProduit().equals(produit))
                .findFirst();

        if (produitBoutiqueOpt.isPresent()) {
            // Si le produit existe déjà dans la boutique, mettre à jour la quantité
            ProduitBoutique produitBoutique = produitBoutiqueOpt.get();
            produitBoutique.setQuantite(produitBoutique.getQuantite() + quantite);
        } else {
            // Si le produit n'existe pas, ajouter un nouveau produit dans la boutique
            ProduitBoutique produitBoutique = new ProduitBoutique();
            produitBoutique.setProduit(produit);
            produitBoutique.setQuantite(quantite);
            produitBoutique.setBoutique(boutique);
            boutique.getProduitsBoutique().add(produitBoutique);
        }

        // Sauvegarder les informations dans la boutique
        boutiqueRepository.save(boutique);

        // Enregistrer le transfert dans l'historique
        TransfertHistorique transfertHistorique = new TransfertHistorique();
        transfertHistorique.setMagasin(magasin);
        transfertHistorique.setBoutique(boutique);
        transfertHistorique.setProduit(produit);
        transfertHistorique.setAncienneQuantite(ancienneQuantite);
        transfertHistorique.setNouvelleQuantite(produit.getQuantite());
        transfertHistorique.setQuantite(quantite);
        transfertHistorique.setDateTransfert(LocalDateTime.now());
        transfertHistoriqueRepository.save(transfertHistorique);
    }

}


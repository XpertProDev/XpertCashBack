package com.xpertcash.service;


import com.xpertcash.entity.Produits;
import com.xpertcash.exceptions.CustomException;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.CategoryProduitRepository;
import com.xpertcash.repository.ProduitsRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ProduitsService {

    @Autowired
    private ProduitsRepository produitsRepository;

    @Autowired
    private  CategoryProduitRepository categoryProduitRepository;

    @Autowired
    private  ImageStorageService imageStorageService;

    // Méthode pour Ajouter un new produit
    public Produits ajouterProduit(Produits produit, MultipartFile imageFile) {
        if (produit.getNomProduit() == null || produit.getNomProduit().isBlank()) {
            throw new NotFoundException("Le nom du produit est obligatoire.");
        }
        if (produit.getPrix() == null || produit.getPrix() <= 0) {
            throw new NotFoundException("Le prix du produit doit être supérieur à 0.");
        }

        // Vérifier si la catégorie existe
        if (produit.getCategory() == null || produit.getCategory().getId() == null) {
            throw new NotFoundException("La catégorie du produit est obligatoire.");
        }

        categoryProduitRepository.findById(produit.getCategory().getId())
                .orElseThrow(() -> new NotFoundException("La catégorie avec l'ID " + produit.getCategory().getId() + " n'existe pas."));

        // Sauvegarde de l'image si fournie
        if (imageFile != null) {
            String imageUrl = imageStorageService.saveImage(imageFile);
            produit.setPhoto(imageUrl);
        }

        return produitsRepository.save(produit);
    }

    // Méthode pour Modifier un produit
    public Produits modifierProduit(Long id, Produits produitModifie, MultipartFile imageFile) {
        Produits produitExistant = produitsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé pour l'ID : " + id));

        // Mettre à jour les champs modifiables
        produitExistant.setNomProduit(produitModifie.getNomProduit());
        produitExistant.setDescription(produitModifie.getDescription());
        produitExistant.setPrix(produitModifie.getPrix());
        produitExistant.setQuantite(produitModifie.getQuantite());
        produitExistant.setSeuil(produitModifie.getSeuil());
        produitExistant.setAlertSeuil(produitModifie.getAlertSeuil());

        // Mise à jour de la catégorie si elle existe
        if (produitModifie.getCategory() != null && produitModifie.getCategory().getId() != null) {
            categoryProduitRepository.findById(produitModifie.getCategory().getId())
                    .orElseThrow(() -> new NotFoundException("Catégorie introuvable avec l'ID : " + produitModifie.getCategory().getId()));
            produitExistant.setCategory(produitModifie.getCategory());
        }

        // Mise à jour de l'image si fournie
        if (imageFile != null) {
            String imageUrl = imageStorageService.saveImage(imageFile);
            produitExistant.setPhoto(imageUrl);
        }

        return produitsRepository.save(produitExistant);
    }

    // Méthode pour récupérer tous les produits
    public List<Produits> getAllProduits() {
        return produitsRepository.findAll();
    }

    // Méthode pour récupérer un produit par son ID
    public Produits getProduitById(Long id) {
        return produitsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé pour l'ID : " + id));
    }

    // Méthode pour supprimer un produit
    public String deleteProduit(Long id) {
        if (produitsRepository.findById(id).isPresent()) {
            produitsRepository.deleteById(id);
            return "Succès : produit supprimé.";
        } else {
            throw new NotFoundException("Produit non trouvé pour l'ID : " + id);
        }
    }

}

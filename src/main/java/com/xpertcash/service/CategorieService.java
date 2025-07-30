package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class CategorieService {
    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private JwtUtil jwtUtil;

     // Ajouter une nouvelle catégorie (seul ADMIN peut le faire)
    public Categorie createCategorie(String nom) {
        if (categorieRepository.existsByNom(nom)) {
            throw new RuntimeException("Cette catégorie existe déjà !");
        }

        Categorie categorie = new Categorie();
        categorie.setNom(nom);
        categorie.setCreatedAt(LocalDateTime.now());
        return categorieRepository.save(categorie);
    }

    // Récupérer toutes les catégories
    public List<Categorie> getCategoriesWithProduitCount(HttpServletRequest request) {
    // 1. Extraire l'utilisateur à partir du token
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur", e);
    }

    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    // 2. Vérification des droits : Assurer que l'utilisateur est Admin ou Manager
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

    if (!isAdminOrManager && !hasPermissionGestionProduits) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour consulter les catégories.");
    }

    // 3. Récupérer toutes les catégories de l'entreprise
    List<Categorie> allCategories = categorieRepository.findAll();

    // 4. Pour chaque catégorie, compter le nombre de produits associés
    for (Categorie categorie : allCategories) {
        long produitCount = produitRepository.countByCategorieIdAndEntrepriseId(categorie.getId(), entreprise.getId());
        categorie.setProduitCount(produitCount);
    }

    return allCategories;
}


     // Supprimer une catégorie
     public void deleteCategorie(Long id) {
        categorieRepository.deleteById(id);
    }

    // Mettre à jour categorie
    public Categorie updateCategorie(HttpServletRequest request, Long categorieId, Categorie categorieDetails) {
        try {
            Categorie categorie = categorieRepository.findById(categorieId)
                    .orElseThrow(() -> new RuntimeException("Categorie non trouvée"));

                    if (categorieRepository.existsByNom(categorieDetails.getNom())) {
                        throw new RuntimeException("Le nom cette categorie existe déjà.");
                    }
            
    
            
            categorie.setNom(categorieDetails.getNom());
    
            // Enregistrer l'unité mise à jour
            return categorieRepository.save(categorie);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du categorie : " + e.getMessage());
        }
    }

}

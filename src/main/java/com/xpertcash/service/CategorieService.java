package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.CategorieResponseDTO;
import com.xpertcash.DTOs.Boutique.BoutiqueResponse;
import com.xpertcash.DTOs.PRODUIT.ProduitDetailsResponseDTO;
import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.User;
import com.xpertcash.entity.UserBoutique;
import com.xpertcash.entity.Enum.TypeProduit;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.EntrepriseRepository;
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

    @Autowired EntrepriseRepository entrepriseRepository;

    @Autowired
    private JwtUtil jwtUtil;

     // Ajouter une nouvelle catégorie (seul ADMIN peut le faire)
  public Categorie createCategorie(String nom, Long entrepriseId) {
    if (categorieRepository.existsByNom(nom)) {
        throw new RuntimeException("Cette catégorie existe déjà !");
    }

    Categorie categorie = new Categorie();
    categorie.setNom(nom);
    categorie.setCreatedAt(LocalDateTime.now());
    
    // Récupérer l'entreprise par son ID et l'assigner à la catégorie
    Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
            .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));

    categorie.setEntreprise(entreprise); // Assurer que cette méthode existe dans l'entité Categorie

    return categorieRepository.save(categorie);
}


    // Récupérer toutes les catégories et ses produits
    public List<CategorieResponseDTO> getCategoriesWithProduitCount(HttpServletRequest request) {
    // 1. Récupérer le token JWT de l'en-tête de la requête
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        // Extraire l'ID utilisateur du token JWT
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur", e);
    }

    // 2. Trouver l'utilisateur dans la base de données
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    // 3. Récupérer l'entreprise de l'utilisateur
    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    // 4. Vérification des droits
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
    boolean isVendeur = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);  // Vérifier si l'utilisateur est un vendeur

    if (!isAdminOrManager && !hasPermissionGestionProduits && !isVendeur) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour consulter les catégories.");
    }

    // 5. Récupérer toutes les catégories pour l'entreprise
    List<Categorie> allCategories = categorieRepository.findAll();

    // 6. Créer une liste de DTOs de catégorie
    List<CategorieResponseDTO> categorieResponseDTOs = new ArrayList<>();

    // 7. Parcourir toutes les catégories et récupérer les produits associés
    for (Categorie categorie : allCategories) {
        // Compter le nombre de produits associés à cette catégorie pour l'entreprise spécifique
        long produitCount = produitRepository.countByCategorieIdAndEntrepriseId(categorie.getId(), entreprise.getId());
        categorie.setProduitCount(produitCount);

        // Filtrer les produits en fonction du rôle de l'utilisateur
        List<ProduitDetailsResponseDTO> produitDTOs = produitRepository.findByCategorieIdAndEntrepriseId(categorie.getId(), entreprise.getId())
                .stream()
                .filter(produit -> {
                     // Exclure les produits supprimés ou inactifs
                    if (Boolean.TRUE.equals(produit.getDeleted()) ) {
                        return false; // Ignorer les produits supprimés ou inactifs
                    }
                    // Si c'est un vendeur, filtrer les produits selon la boutique assignée
                    if (isVendeur && produit.getBoutique() != null) {
                        // Récupérer les boutiques assignées à l'utilisateur
                        List<UserBoutique> userBoutiques = user.getUserBoutiques();
                        if (userBoutiques != null && !userBoutiques.isEmpty()) {
                            // Si la boutique du produit est celle assignée à l'utilisateur, alors l'afficher
                            return produit.getBoutique().getId().equals(userBoutiques.get(0).getBoutique().getId());
                        }
                    }
                    return !produit.getTypeProduit().equals(TypeProduit.SERVICE);
                })
                .map(produit -> {
                    // Vérification si l'unité de mesure est non nulle avant d'y accéder
                    Long uniteId = produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getId() : null;
                    String uniteNom = produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getNom() : "Non spécifiée";

                    // Retourner un DTO de produit
                    return new ProduitDetailsResponseDTO(
                            produit.getId(),
                            produit.getNom(),
                            produit.getPrixVente(),
                            produit.getPrixAchat(),
                            produit.getQuantite(),
                            produit.getSeuilAlert(),
                            produit.getCategorie().getId(),
                            uniteId,
                            produit.getCodeBare(),
                            produit.getPhoto(),
                            produit.getEnStock(),
                            produit.getCategorie().getNom(),
                            uniteNom,
                            produit.getTypeProduit().name(),
                            produit.getCreatedAt(),
                            produit.getLastUpdated(),
                            produit.getDatePreemption(),
                            produit.getBoutique() != null ? produit.getBoutique().getId() : null,
                            produit.getBoutique() != null ? produit.getBoutique().getNomBoutique() : null
                    );
                })
                .collect(Collectors.toList());

        // Créer un DTO de catégorie avec les produits associés
        CategorieResponseDTO categorieDTO = new CategorieResponseDTO(categorie);
        categorieDTO.setProduits(produitDTOs);

        // Ajouter à la liste de réponses
        categorieResponseDTOs.add(categorieDTO);
    }

    // 8. Retourner la liste des catégories avec les produits associés
    return categorieResponseDTOs;
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

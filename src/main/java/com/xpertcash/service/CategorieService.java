package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        // --- JWT & utilisateur inchangé ---
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) throw new RuntimeException("Aucune entreprise associée");

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean isVendeur = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

        if (!isAdminOrManager && !hasPermissionGestionProduits && !isVendeur) {
            throw new RuntimeException("Accès refusé");
        }

        // --- Récupérer les catégories et produits ---
        List<Categorie> allCategories = categorieRepository.findAll();
        List<Produit> allProduits = produitRepository.findAllWithCategorieAndBoutiqueByEntrepriseId(entreprise.getId());

        // --- Récupérer le count groupé par catégorie ---
        Map<Long, Long> produitCountMap = produitRepository.countProduitsParCategorie(entreprise.getId())
                .stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> (Long) obj[1]
                ));

        // --- Grouper les produits par catégorie ---
        Map<Long, List<Produit>> produitsParCategorie = allProduits.stream()
                .collect(Collectors.groupingBy(p -> p.getCategorie().getId()));

        List<CategorieResponseDTO> categorieResponseDTOs = new ArrayList<>();
        for (Categorie categorie : allCategories) {
            // set le count directement depuis la DB
            categorie.setProduitCount(produitCountMap.getOrDefault(categorie.getId(), 0L));

            List<ProduitDetailsResponseDTO> produitDTOs = produitsParCategorie.getOrDefault(categorie.getId(), Collections.emptyList())
                    .stream()
                    .filter(produit -> {
                        if (Boolean.TRUE.equals(produit.getDeleted())) return false;
                        if (isVendeur && produit.getBoutique() != null) {
                            List<UserBoutique> userBoutiques = user.getUserBoutiques();
                            if (userBoutiques != null && !userBoutiques.isEmpty()) {
                                return produit.getBoutique().getId().equals(userBoutiques.get(0).getBoutique().getId());
                            }
                        }
                        return !TypeProduit.SERVICE.equals(produit.getTypeProduit());
                    })
                    .map(this::toProduitDTO)  // mapping vers DTO dans méthode privée
                    .collect(Collectors.toList());

            CategorieResponseDTO categorieDTO = new CategorieResponseDTO(categorie);
            categorieDTO.setProduits(produitDTOs);
            categorieResponseDTOs.add(categorieDTO);
        }

        return categorieResponseDTOs;
    }

    // Méthode privée pour mapping DTO
    private ProduitDetailsResponseDTO toProduitDTO(Produit produit) {
        Long uniteId = produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getId() : null;
        String uniteNom = produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getNom() : "Non spécifiée";

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
                produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null,
                produit.getCreatedAt(),
                produit.getLastUpdated(),
                produit.getDatePreemption(),
                produit.getBoutique() != null ? produit.getBoutique().getId() : null,
                produit.getBoutique() != null ? produit.getBoutique().getNomBoutique() : null
        );
    }

/* Méthode privée pour mapper un produit vers son DTO
private ProduitDetailsResponseDTO toProduitDTO(Produit produit) {
    Long uniteId = produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getId() : null;
    String uniteNom = produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getNom() : "Non spécifiée";

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
        produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null,
        produit.getCreatedAt(),
        produit.getLastUpdated(),
        produit.getDatePreemption(),
        produit.getBoutique() != null ? produit.getBoutique().getId() : null,
        produit.getBoutique() != null ? produit.getBoutique().getNomBoutique() : null
    );
}
 */
   


     // Supprimer une catégorie
   public void supprimerCategorieSiVide(Long categorieId, HttpServletRequest request) {
    // 1. Récupérer l'utilisateur depuis le token
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    // 2. Vérifier l'appartenance à une entreprise
    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    // 3. Vérifier les droits d'accès
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    if (!isAdminOrManager) {
        throw new RuntimeException("Accès refusé : seuls les administrateurs ou managers peuvent supprimer une catégorie.");
    }

    // 4. Vérifier que la catégorie existe
    Categorie categorie = categorieRepository.findById(categorieId)
            .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));

    // 5. Vérifier que la catégorie est bien liée à des produits de l'entreprise
    // Optimisation : utilisation de la même requête countByCategorieIdAndEntrepriseId
    long produitCount = produitRepository.countByCategorieIdAndEntrepriseId(categorieId, entreprise.getId());
    if (produitCount > 0) {
        throw new RuntimeException("Impossible de supprimer une catégorie contenant des produits.");
    }

    // 6. Supprimer la catégorie
    categorieRepository.delete(categorie);
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

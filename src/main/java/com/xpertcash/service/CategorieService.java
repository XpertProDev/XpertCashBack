package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.CategorieResponseDTO;
import com.xpertcash.DTOs.CategoriePaginatedResponseDTO;
import com.xpertcash.DTOs.ProduitPaginatedResponseDTO;
import com.xpertcash.DTOs.PRODUIT.ProduitDetailsResponseDTO;
import com.xpertcash.configuration.CentralAccess;
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

import jakarta.servlet.http.HttpServletRequest;
import com.xpertcash.service.AuthenticationHelper;

@Service
public class CategorieService {

    @Autowired
    private AuthenticationHelper authHelper;
    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired EntrepriseRepository entrepriseRepository;

     // Ajouter une nouvelle catégorie (seul ADMIN peut le faire)
  @CacheEvict(value = "categories", allEntries = true)
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


    // Récupérer toutes les catégories avec comptage des produits (sans pagination)
    @Cacheable(value = "categories", key = "#request.getHeader('Authorization').hashCode() + '_all_categories'")
    public List<CategorieResponseDTO> getAllCategoriesWithProduitCount(HttpServletRequest request) {
        // --- JWT & utilisateur inchangé ---
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) throw new RuntimeException("Aucune entreprise associée");

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean isVendeur = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

        if (!isAdminOrManager && !hasPermissionGestionProduits && !isVendeur) {
            throw new RuntimeException("Accès refusé");
        }

        // --- Récupérer toutes les catégories de l'entreprise ---
        List<Categorie> allCategories = categorieRepository.findByEntrepriseId(entreprise.getId());

        // --- Récupérer le count groupé par catégorie ---
        Map<Long, Long> produitCountMap = produitRepository.countProduitsParCategorie(entreprise.getId())
                .stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> (Long) obj[1]
                ));

        // --- Construire la réponse sans les produits (seulement le comptage) ---
        List<CategorieResponseDTO> categorieResponseDTOs = new ArrayList<>();
        for (Categorie categorie : allCategories) {
            // set le count directement depuis la DB
            categorie.setProduitCount(produitCountMap.getOrDefault(categorie.getId(), 0L));

            CategorieResponseDTO categorieDTO = new CategorieResponseDTO(categorie);
            // Ne pas charger les produits ici - ils seront chargés séparément avec pagination
            categorieDTO.setProduits(Collections.emptyList());
            categorieResponseDTOs.add(categorieDTO);
        }

        return categorieResponseDTOs;
    }

    // Récupérer les produits d'une catégorie spécifique avec pagination
    public ProduitPaginatedResponseDTO getProduitsByCategoriePaginated(
            Long categorieId, 
            int page, 
            int size, 
            HttpServletRequest request) {
        
        // --- JWT & utilisateur inchangé ---
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) throw new RuntimeException("Aucune entreprise associée");

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean isVendeur = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

        if (!isAdminOrManager && !hasPermissionGestionProduits && !isVendeur) {
            throw new RuntimeException("Accès refusé");
        }

        // --- Vérifier que la catégorie existe et appartient à l'entreprise ---
        categorieRepository.findByIdAndEntrepriseId(categorieId, entreprise.getId())
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable ou non autorisée"));

        // --- Validation des paramètres de pagination ---
        if (page < 0) page = 0;
        if (size <= 0) size = 20; // Taille par défaut
        if (size > 100) size = 100; // Limite maximale pour éviter la surcharge

        // --- Pagination des produits de la catégorie ---
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        Page<Produit> produitsPage = produitRepository.findByCategorieIdAndEntrepriseIdPaginated(
                categorieId, entreprise.getId(), pageable);

        // --- Filtrer et mapper les produits selon les permissions ---
        List<ProduitDetailsResponseDTO> produitDTOs = produitsPage.getContent().stream()
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
                .map(this::toProduitDTO)
                .collect(Collectors.toList());

        // --- Créer la page de DTOs ---
        Page<ProduitDetailsResponseDTO> dtoPage = new PageImpl<>(
                produitDTOs, 
                pageable, 
                produitsPage.getTotalElements()
        );

        return ProduitPaginatedResponseDTO.fromPage(dtoPage);
    }

    // Méthode de compatibilité (maintenue pour l'ancienne API)
    public List<CategorieResponseDTO> getCategoriesWithProduitCount(HttpServletRequest request) {
        return getAllCategoriesWithProduitCount(request);
    }

    // Récupérer toutes les catégories et ses produits avec pagination (méthode scalable pour SaaS)
    @Cacheable(value = "categories", key = "#request.getHeader('Authorization').hashCode() + '_paginated_' + #page + '_' + #size")
    public CategoriePaginatedResponseDTO getCategoriesWithProduitCountPaginated(HttpServletRequest request, int page, int size) {
        // --- JWT & utilisateur inchangé ---
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) throw new RuntimeException("Aucune entreprise associée");

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean isVendeur = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

        if (!isAdminOrManager && !hasPermissionGestionProduits && !isVendeur) {
            throw new RuntimeException("Accès refusé");
        }

        // --- Validation des paramètres de pagination ---
        if (page < 0) page = 0;
        if (size <= 0) size = 20; // Taille par défaut
        if (size > 100) size = 100; // Limite maximale pour éviter la surcharge

        // --- Récupérer les catégories avec pagination ---
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        Page<Categorie> categoriesPage = categorieRepository.findByEntrepriseId(entreprise.getId(), pageable);

        // --- Récupérer le count groupé par catégorie pour les catégories de la page ---
        List<Long> categorieIds = categoriesPage.getContent().stream()
                .map(Categorie::getId)
                .collect(Collectors.toList());

        Map<Long, Long> produitCountMap = new HashMap<>();
        if (!categorieIds.isEmpty()) {
            produitCountMap = produitRepository.countProduitsParCategorieIds(entreprise.getId(), categorieIds)
                    .stream()
                    .collect(Collectors.toMap(
                            obj -> (Long) obj[0],
                            obj -> (Long) obj[1]
                    ));
        }

        // --- Récupérer les produits pour les catégories de la page seulement ---
        Map<Long, List<Produit>> produitsParCategorie = new HashMap<>();
        if (!categorieIds.isEmpty()) {
            List<Produit> produits = produitRepository.findByCategorieIdsAndEntrepriseId(categorieIds, entreprise.getId());
            produitsParCategorie = produits.stream()
                    .collect(Collectors.groupingBy(p -> p.getCategorie().getId()));
        }

        // --- Construire la réponse paginée ---
        List<CategorieResponseDTO> categorieResponseDTOs = new ArrayList<>();
        for (Categorie categorie : categoriesPage.getContent()) {
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

        // --- Créer la page de DTOs ---
        Page<CategorieResponseDTO> dtoPage = new PageImpl<>(
                categorieResponseDTOs, 
                pageable, 
                categoriesPage.getTotalElements()
        );

        return CategoriePaginatedResponseDTO.fromPage(dtoPage);
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
   @CacheEvict(value = "categories", allEntries = true)
   public void supprimerCategorieSiVide(Long categorieId, HttpServletRequest request) {
    // 1. Récupérer l'utilisateur depuis le token
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

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
    @CacheEvict(value = "categories", allEntries = true)
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

    // Méthode simple pour obtenir le nombre de produits par catégorie (OPTIMISÉE)
    public List<Map<String, Object>> getCategoriesWithProductCount(HttpServletRequest request) {
        // --- JWT & utilisateur inchangé ---
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) throw new RuntimeException("Aucune entreprise associée");

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean isVendeur = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

        if (!isAdminOrManager && !hasPermissionGestionProduits && !isVendeur) {
            throw new RuntimeException("Accès refusé");
        }

        // --- Récupérer toutes les catégories de l'entreprise ---
        List<Categorie> allCategories = categorieRepository.findByEntrepriseId(entreprise.getId());

        // --- Récupérer le count groupé par catégorie (OPTIMISÉ avec filtre deleted) ---
        Map<Long, Long> produitCountMap = produitRepository.countProduitsParCategorie(entreprise.getId())
                .stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> (Long) obj[1]
                ));

        // --- Construire la réponse simple ---
        List<Map<String, Object>> result = new ArrayList<>();
        for (Categorie categorie : allCategories) {
            Map<String, Object> categorieInfo = new HashMap<>();
            categorieInfo.put("id", categorie.getId());
            categorieInfo.put("nom", categorie.getNom());
            categorieInfo.put("produitCount", produitCountMap.getOrDefault(categorie.getId(), 0L));
            categorieInfo.put("createdAt", categorie.getCreatedAt());
            result.add(categorieInfo);
        }

        return result;
    }

// ==================== MÉTHODES D'INVALIDATION DU CACHE CATÉGORIES ====================

/**
 * Invalide le cache des catégories
 */
@CacheEvict(value = "categories", allEntries = true)
public void evictCategoriesCache() {
    // Méthode pour vider le cache des catégories
    System.out.println("🔄 Cache des catégories vidé");
}

}

package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.ProduitRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class CategorieService {

    @Autowired
    private AuthenticationHelper authHelper;
    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

  public Categorie createCategorie(String nom, HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);
    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    if (categorieRepository.existsByNomAndEntrepriseId(nom, entreprise.getId())) {
        throw new RuntimeException("Cette catégorie existe déjà pour votre entreprise !");
    }

    Categorie categorie = new Categorie();
    categorie.setNom(nom);
    categorie.setCreatedAt(LocalDateTime.now());
    categorie.setOrigineCreation("PRODUIT");
    categorie.setEntreprise(entreprise);

    return categorieRepository.save(categorie);
}


    // Récupérer toutes les catégories avec comptage des produits (sans pagination)
    public List<CategorieResponseDTO> getAllCategoriesWithProduitCount(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) throw new RuntimeException("Aucune entreprise associée");

        // boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        // boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        // boolean isVendeur = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

        // if (!isAdminOrManager && !hasPermissionGestionProduits && !isVendeur) {
        //     throw new RuntimeException("Accès refusé");
        // }

        List<Categorie> allCategories = categorieRepository.findByEntrepriseId(entreprise.getId());

        Map<Long, Long> produitCountMap = produitRepository.countProduitsParCategorie(entreprise.getId())
                .stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> (Long) obj[1]
                ));

        List<CategorieResponseDTO> categorieResponseDTOs = new ArrayList<>();
        for (Categorie categorie : allCategories) {
            categorie.setProduitCount(produitCountMap.getOrDefault(categorie.getId(), 0L));

            CategorieResponseDTO categorieDTO = new CategorieResponseDTO(categorie);
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

        categorieRepository.findByIdAndEntrepriseId(categorieId, entreprise.getId())
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable ou non autorisée"));

        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        // Favoris pour la vente en premier, puis ordreFavori, puis nom
        Sort sort = Sort.by(
                Sort.Order.desc("favoriPourVente"),
                Sort.Order.asc("ordreFavori").nullsLast()
        ).and(Sort.by("nom").ascending());
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Produit> produitsPage = produitRepository.findByCategorieIdAndEntrepriseIdPaginated(
                categorieId, entreprise.getId(), pageable);

        List<ProduitDetailsResponseDTO> produitDTOs = produitsPage.getContent().stream()
                .filter(produit -> {
                    if (Boolean.TRUE.equals(produit.getDeleted())) return false;
                    if (isAdminOrManager) {
                        return !TypeProduit.SERVICE.equals(produit.getTypeProduit());
                    }
                    if (isVendeur && produit.getBoutique() != null) {
                        List<UserBoutique> userBoutiques = user.getUserBoutiques();
                        if (userBoutiques != null && !userBoutiques.isEmpty()) {
                            return userBoutiques.stream()
                                    .anyMatch(ub -> ub.getBoutique().getId().equals(produit.getBoutique().getId()));
                        }
                    }
                    return !TypeProduit.SERVICE.equals(produit.getTypeProduit());
                })
                .map(this::toProduitDTO)
                .collect(Collectors.toList());

        Page<ProduitDetailsResponseDTO> dtoPage = new PageImpl<>(
                produitDTOs, 
                pageable, 
                produitsPage.getTotalElements()
        );

        return ProduitPaginatedResponseDTO.fromPage(dtoPage);
    }

    public List<CategorieResponseDTO> getCategoriesWithProduitCount(HttpServletRequest request) {
        return getAllCategoriesWithProduitCount(request);
    }

    // Récupérer toutes les catégories et ses produits avec pagination (méthode scalable)
    public CategoriePaginatedResponseDTO getCategoriesWithProduitCountPaginated(HttpServletRequest request, int page, int size) {
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

        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        Page<Categorie> categoriesPage = categorieRepository.findByEntrepriseId(entreprise.getId(), pageable);

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

        Map<Long, List<Produit>> produitsParCategorie = new HashMap<>();
        if (!categorieIds.isEmpty()) {
            List<Produit> produits = produitRepository.findByCategorieIdsAndEntrepriseId(categorieIds, entreprise.getId());
            produitsParCategorie = produits.stream()
                    .collect(Collectors.groupingBy(p -> p.getCategorie().getId()));
        }

        List<CategorieResponseDTO> categorieResponseDTOs = new ArrayList<>();
        for (Categorie categorie : categoriesPage.getContent()) {
            categorie.setProduitCount(produitCountMap.getOrDefault(categorie.getId(), 0L));

            List<ProduitDetailsResponseDTO> produitDTOs = produitsParCategorie.getOrDefault(categorie.getId(), Collections.emptyList())
                    .stream()
                    .filter(produit -> {
                        if (Boolean.TRUE.equals(produit.getDeleted())) return false;
                        if (isAdminOrManager) {
                            return !TypeProduit.SERVICE.equals(produit.getTypeProduit());
                        }
                        if (isVendeur && produit.getBoutique() != null) {
                            List<UserBoutique> userBoutiques = user.getUserBoutiques();
                            if (userBoutiques != null && !userBoutiques.isEmpty()) {
                                return userBoutiques.stream()
                                        .anyMatch(ub -> ub.getBoutique().getId().equals(produit.getBoutique().getId()));
                            }
                        }
                        return !TypeProduit.SERVICE.equals(produit.getTypeProduit());
                    })
                    .map(this::toProduitDTO)
                    .collect(Collectors.toList());

            CategorieResponseDTO categorieDTO = new CategorieResponseDTO(categorie);
            categorieDTO.setProduits(produitDTOs);
            categorieResponseDTOs.add(categorieDTO);
        }

        Page<CategorieResponseDTO> dtoPage = new PageImpl<>(
                categorieResponseDTOs, 
                pageable, 
                categoriesPage.getTotalElements()
        );

        return CategoriePaginatedResponseDTO.fromPage(dtoPage);
    }

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
                produit.getBoutique() != null ? produit.getBoutique().getNomBoutique() : null,
                Boolean.TRUE.equals(produit.getFavoriPourVente()),
                produit.getOrdreFavori()
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
        produit.getBoutique() != null ? produit.getBoutique().getNomBoutique() : null,
        Boolean.TRUE.equals(produit.getFavoriPourVente()),
        produit.getOrdreFavori()
    );
}
 */
   


     // Supprimer une catégorie
   public void supprimerCategorieSiVide(Long categorieId, HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    if (!isAdminOrManager) {
        throw new RuntimeException("Accès refusé : seuls les administrateurs ou managers peuvent supprimer une catégorie.");
    }

    Categorie categorie = categorieRepository.findById(categorieId)
            .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));


    long produitCount = produitRepository.countByCategorieIdAndEntrepriseId(categorieId, entreprise.getId());
    if (produitCount > 0) {
        throw new RuntimeException("Impossible de supprimer une catégorie contenant des produits.");
    }

    categorieRepository.delete(categorie);
}


    // Mettre à jour categorie
    public Categorie updateCategorie(HttpServletRequest request, Long categorieId, Categorie categorieDetails) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                throw new RuntimeException("Token JWT manquant ou mal formaté");
            }

            User user = authHelper.getAuthenticatedUserWithFallback(request);
            Entreprise entreprise = user.getEntreprise();
            if (entreprise == null) {
                throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
            }

            Categorie categorie = categorieRepository.findByIdAndEntrepriseId(categorieId, entreprise.getId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable ou non autorisée"));

            if (!categorie.getNom().equals(categorieDetails.getNom()) && 
                categorieRepository.existsByNomAndEntrepriseId(categorieDetails.getNom(), entreprise.getId())) {
                throw new RuntimeException("Le nom de cette catégorie existe déjà pour votre entreprise.");
            }
            
            categorie.setNom(categorieDetails.getNom());
    
            return categorieRepository.save(categorie);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du categorie : " + e.getMessage());
        }
    }

    // Méthode simple pour obtenir le nombre de produits par catégorie (OPTIMISÉE)
    public List<Map<String, Object>> getCategoriesWithProductCount(HttpServletRequest request) {
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

        List<Categorie> allCategories = categorieRepository.findByEntrepriseId(entreprise.getId());

        Map<Long, Long> produitCountMap = produitRepository.countProduitsParCategorieExcluantService(entreprise.getId())
                .stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> (Long) obj[1]
                ));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Categorie categorie : allCategories) {
            Map<String, Object> categorieInfo = new HashMap<>();
            categorieInfo.put("id", categorie.getId());
            categorieInfo.put("nom", categorie.getNom());
            categorieInfo.put("produitCount", produitCountMap.getOrDefault(categorie.getId(), 0L));
            categorieInfo.put("createdAt", categorie.getCreatedAt());
            categorieInfo.put("origineCreation", categorie.getOrigineCreation());
            result.add(categorieInfo);
        }

        return result;
    }

    /**
     * Catégories avec nombre de produits par boutique (isolation multi-tenant).
     * Valide que la boutique appartient à l'entreprise de l'utilisateur.
     * Une seule requête de comptage par boutique (optimisé).
     */
    public List<Map<String, Object>> getCategoriesWithProductCountByBoutique(Long boutiqueId, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean isVendeur = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermissionGestionProduits && !isVendeur) {
            throw new RuntimeException("Accès refusé");
        }

        // Isolation multi-tenant : la boutique doit appartenir à l'entreprise
        boutiqueRepository.findByIdAndEntrepriseId(boutiqueId, entreprise.getId())
                .orElseThrow(() -> new RuntimeException("Boutique introuvable ou n'appartient pas à votre entreprise"));

        List<Categorie> categories = categorieRepository.findByEntrepriseId(entreprise.getId());
        Map<Long, Long> produitCountMap = produitRepository.countProduitsParCategorieByBoutique(boutiqueId)
                .stream()
                .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> (Long) obj[1]));

        List<Map<String, Object>> result = new ArrayList<>(categories.size());
        for (Categorie c : categories) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("nom", c.getNom());
            row.put("produitCount", produitCountMap.getOrDefault(c.getId(), 0L));
            row.put("createdAt", c.getCreatedAt());
            row.put("origineCreation", c.getOrigineCreation());
            result.add(row);
        }
        return result;
    }

}

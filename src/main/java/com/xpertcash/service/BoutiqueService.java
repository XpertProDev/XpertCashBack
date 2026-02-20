package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.TransfertDTO;
import com.xpertcash.configuration.CentralAccess;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.StockHistory;
import com.xpertcash.entity.Transfert;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.TypeBoutique;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.LigneFactureProformaRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockHistoryRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.TransfertRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class BoutiqueService {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private BoutiqueRepository boutiqueRepository;


    @Autowired
    private TransfertRepository transfertRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private LigneFactureProformaRepository ligneFactureProformaRepository;


    // Ajouter une nouvelle boutique pour l'admin
    @Transactional
    public Boutique ajouterBoutique(HttpServletRequest request, String nomBoutique, String adresse, String Telephone, String email, TypeBoutique typeBoutique) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User admin = authHelper.getAuthenticatedUserWithFallback(request);

        RoleType role = admin.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Seul un admin ou un manager peut ajouter une boutique !");
        }
        

        if (admin.getEntreprise() == null) {
            throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
        }

        Boutique boutique = new Boutique();
        boutique.setNomBoutique(nomBoutique);
        boutique.setAdresse(adresse);
        boutique.setTelephone(Telephone);
        boutique.setEmail(email);
        boutique.setEntreprise(admin.getEntreprise());
        boutique.setCreatedAt(LocalDateTime.now());
        boutique.setTypeBoutique(typeBoutique);

        return boutiqueRepository.save(boutique);
    }

    public List<Boutique> getBoutiquesByEntreprise(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    if (user.getEntreprise() == null) {
        throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
    }

    Long entrepriseId = user.getEntreprise().getId();

    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);
    boolean hasVendrePermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

    if (!isAdminOrManager && !hasPermission && !hasVendrePermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour consulter les boutiques de cette entreprise !");
    }

    if (hasVendrePermission && !isAdminOrManager && !hasPermission) {
        return user.getUserBoutiques().stream()
                .map(userBoutique -> userBoutique.getBoutique())
                .filter(boutique -> boutique.getEntreprise().getId().equals(entrepriseId))
                .collect(Collectors.toList());
    }

    return boutiqueRepository.findByEntrepriseId(entrepriseId);
}

    public Boutique getBoutiqueById(Long boutiqueId, HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    Long entrepriseId = boutique.getEntreprise().getId();


    if (!user.getEntreprise().getId().equals(entrepriseId)) {
        throw new RuntimeException("Vous n'avez pas accès à cette boutique (entreprise différente).");
    }

    return boutique;
}

    public Boutique updateBoutique(Long boutiqueId, String newNomBoutique, String newAdresse, String newTelephone, String newEmail, HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    Long entrepriseId = boutique.getEntreprise().getId();

    boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

    if (!isAdmin && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour modifier cette boutique !");
    }

    if (newNomBoutique != null) boutique.setNomBoutique(newNomBoutique);
    if (newAdresse != null) boutique.setAdresse(newAdresse);
    if (newTelephone != null) boutique.setTelephone(newTelephone);
    if (newEmail != null) boutique.setEmail(newEmail);
    boutique.setLastUpdated(LocalDateTime.now());

    return boutiqueRepository.save(boutique);
}

    @Transactional
    public void transfererProduits(HttpServletRequest request, Long boutiqueSourceId, Long boutiqueDestinationId, Long produitId, int quantite) {

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);


         if (user.getEntreprise() == null) {
        throw new RuntimeException("Utilisateur non rattaché à une entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                       || user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);

   
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour effectuer les transferts !");
        }

        Boutique boutiqueSource = boutiqueRepository.findById(boutiqueSourceId)
                .orElseThrow(() -> new RuntimeException("Boutique source non trouvée"));
        Boutique boutiqueDestination = boutiqueRepository.findById(boutiqueDestinationId)
                .orElseThrow(() -> new RuntimeException("Boutique destination non trouvée"));

        if (!boutiqueSource.isActif() || !boutiqueDestination.isActif()) {
            throw new RuntimeException("L'une des boutiques est désactivée !");
        }

            if (!boutiqueSource.getEntreprise().getId().equals(entrepriseId) ||
            !boutiqueDestination.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Transfert interdit : les deux boutiques doivent appartenir à votre entreprise.");
         }

        Produit produit = produitRepository.findByBoutiqueAndId(boutiqueSourceId, produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé dans la boutique source"));

        if (produit.getQuantite() < quantite) {
            throw new RuntimeException("Quantité insuffisante dans la boutique source !");
        }

        produit.setQuantite(produit.getQuantite() - quantite);
        produitRepository.save(produit);

        Optional<Produit> produitDestinationOpt = produitRepository.findByBoutiqueAndCodeGenerique(
                boutiqueDestination.getId(), produit.getCodeGenerique());

        Produit produitDestination;
        if (produitDestinationOpt.isPresent()) {
            produitDestination = produitDestinationOpt.get();
            produitDestination.setQuantite(produitDestination.getQuantite() + quantite);
        } else {
            produitDestination = new Produit();
            produitDestination.setNom(produit.getNom());
            produitDestination.setPrixVente(produit.getPrixVente());
            produitDestination.setPrixAchat(produit.getPrixAchat());
            produitDestination.setQuantite(quantite);
            produitDestination.setCodeGenerique(produit.getCodeGenerique());
            produitDestination.setCodeBare(produit.getCodeBare());
            produitDestination.setPhoto(produit.getPhoto());
            produitDestination.setCategorie(produit.getCategorie());
            produitDestination.setUniteDeMesure(produit.getUniteDeMesure());
            produitDestination.setCreatedAt(produit.getCreatedAt());
            produitDestination.setLastUpdated(produit.getLastUpdated());
            produitDestination.setBoutique(boutiqueDestination);
        }

        produitDestination.setEnStock(true);
        produitRepository.save(produitDestination);

        Stock stock = stockRepository.findByProduit(produitDestination);
        if (stock == null) {
            stock = new Stock();
            stock.setProduit(produitDestination);
            stock.setStockActuel(produitDestination.getQuantite());
            stock.setQuantiteAjoute(quantite);
            stock.setStockApres(produitDestination.getQuantite());
            stock.setLastUpdated(LocalDateTime.now());
        } else {
            int stockAvant = stock.getStockActuel();
            int stockApres = stockAvant + quantite;
            stock.setStockActuel(stockApres);
            stock.setQuantiteAjoute(quantite);
            stock.setStockApres(stockApres);
            stock.setLastUpdated(LocalDateTime.now());
        }
        stockRepository.save(stock);

        StockHistory history = new StockHistory();
        history.setAction("Transfert depuis boutique " + boutiqueSource.getNomBoutique());
        history.setQuantite(quantite);
        history.setStockAvant(stock.getStockApres() - quantite);
        history.setStockApres(stock.getStockApres());
        history.setDescription("Transfert automatique via fonctionnalité de transfert");
        history.setCreatedAt(LocalDateTime.now());
        history.setStock(stock);
        history.setUser(user);
        stockHistoryRepository.save(history);

        Transfert transfert = new Transfert();
        transfert.setProduit(produit);
        transfert.setBoutiqueSource(boutiqueSource);
        transfert.setBoutiqueDestination(boutiqueDestination);
        transfert.setQuantite(quantite);
        transfertRepository.save(transfert);
    }

    //Copie
    @Transactional
    public int copierProduits(HttpServletRequest request, Long boutiqueSourceId, Long boutiqueDestinationId, List<Long> listeProduitIds, boolean toutCopier) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Boutique boutiqueSource = boutiqueRepository.findById(boutiqueSourceId)
                .orElseThrow(() -> new RuntimeException("Boutique source non trouvée"));
        Boutique boutiqueDestination = boutiqueRepository.findById(boutiqueDestinationId)
                .orElseThrow(() -> new RuntimeException("Boutique destination non trouvée"));

        Long entrepriseId = boutiqueSource.getEntreprise().getId();

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId);
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                             || user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour copier des produits !");
        }

        if (!boutiqueSource.getEntreprise().getId().equals(entrepriseId)
            || !boutiqueDestination.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Les boutiques doivent appartenir à la même entreprise !");
        }

        if (!boutiqueSource.isActif() || !boutiqueDestination.isActif()) {
            throw new RuntimeException("Les boutiques doivent être actives !");
        }

        List<Produit> produitsACopier = toutCopier
                ? produitRepository.findByBoutique(boutiqueSource)
                : produitRepository.findByBoutiqueAndIdIn(boutiqueSourceId, listeProduitIds);

        if (produitsACopier == null || produitsACopier.isEmpty()) {
            throw new RuntimeException("Aucun produit à copier depuis la boutique source !");
        }

        int compteurProduitsCopies = 0;

        for (Produit produit : produitsACopier) {
            boolean existeDeja = produitRepository
                    .findByBoutiqueAndCodeGenerique(boutiqueDestination.getId(), produit.getCodeGenerique())
                    .isPresent();

            if (existeDeja) continue;

            Produit nouveauProduit = new Produit();
            nouveauProduit.setNom(produit.getNom());
            nouveauProduit.setPrixVente(produit.getPrixVente());
            nouveauProduit.setPrixAchat(produit.getPrixAchat());
            nouveauProduit.setQuantite(0);
            nouveauProduit.setDescription(produit.getDescription());
            nouveauProduit.setCodeGenerique(produit.getCodeGenerique());
            nouveauProduit.setCodeBare(produit.getCodeBare());
            nouveauProduit.setPhoto(produit.getPhoto());
            nouveauProduit.setCategorie(produit.getCategorie());
            nouveauProduit.setUniteDeMesure(produit.getUniteDeMesure());
            nouveauProduit.setCreatedAt(LocalDateTime.now());
            nouveauProduit.setLastUpdated(LocalDateTime.now());
            nouveauProduit.setEnStock(true);
            nouveauProduit.setBoutique(boutiqueDestination);

            produitRepository.save(nouveauProduit);

            Stock stockDestination = new Stock();
            stockDestination.setProduit(nouveauProduit);
            stockDestination.setStockActuel(0);
            stockDestination.setQuantiteAjoute(0);
            stockDestination.setStockApres(0);
            stockDestination.setLastUpdated(LocalDateTime.now());

            stockRepository.save(stockDestination);

            compteurProduitsCopies++;
        }

        if (compteurProduitsCopies == 0) {
            throw new RuntimeException("Aucun produit n'a été copié : les produits existent déjà dans la boutique destination.");
        }

        return compteurProduitsCopies;
    }

   
   public List<ProduitDTO> getProduitsParBoutique(HttpServletRequest request, Long boutiqueId) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        Long entrepriseId = boutique.getEntreprise().getId();

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId);
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour accéder aux produits de cette boutique.");
        }

        if (!user.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Vous n'avez pas accès à cette boutique (entreprise différente).");
        }

        if (!boutique.isActif()) {
            throw new RuntimeException("Cette boutique est désactivée, ses produits ne sont pas accessibles !");
        }

        List<Produit> produits = produitRepository.findByBoutiqueIdAndNotDeleted(boutiqueId);

        List<ProduitDTO> produitsDTO = produits.stream().map(produit -> {
            ProduitDTO produitDTO = new ProduitDTO();
            produitDTO.setId(produit.getId());
            produitDTO.setNom(produit.getNom());
            produitDTO.setPrixVente(produit.getPrixVente());
            produitDTO.setPrixAchat(produit.getPrixAchat());
            produitDTO.setQuantite(produit.getQuantite());
            produitDTO.setSeuilAlert(produit.getSeuilAlert());
            produitDTO.setDescription(produit.getDescription());
            produitDTO.setCodeGenerique(produit.getCodeGenerique());
            produitDTO.setCodeBare(produit.getCodeBare());
            produitDTO.setPhoto(produit.getPhoto());
            produitDTO.setEnStock(produit.getEnStock());
            produitDTO.setCreatedAt(produit.getCreatedAt());
            produitDTO.setLastUpdated(produit.getLastUpdated());
            produitDTO.setDatePreemption(produit.getDatePreemption());
            //  produitDTO.setTypeProduit(produit.getTypeProduit().name());

            if (produit.getCategorie() != null) {
                produitDTO.setNomCategorie(produit.getCategorie().getNom());
                produitDTO.setCategorieId(produit.getCategorie().getId());
            }

            if (produit.getUniteDeMesure() != null) {
                produitDTO.setNomUnite(produit.getUniteDeMesure().getNom());
                produitDTO.setUniteId(produit.getUniteDeMesure().getId());
            }

            Map<String, Object> boutiqueMap = new HashMap<>();
            boutiqueMap.put("id", boutique.getId());
            boutiqueMap.put("nom", boutique.getNomBoutique());
            produitDTO.setBoutiques(Collections.singletonList(boutiqueMap));

            return produitDTO;
        }).collect(Collectors.toList());

        return produitsDTO;
    }

    // Methode pour descativer une boutique
     public Boutique desactiverBoutique(Long boutiqueId, HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    Long entrepriseId = boutique.getEntreprise().getId();

    boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.DESACTIVER_BOUTIQUE);

    if (!isAdmin && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour désactiver cette boutique !");
    }

    boutique.setActif(false);
    boutique.setLastUpdated(LocalDateTime.now());

    return boutiqueRepository.save(boutique);
}

    // Methode pour activer une boutique
    public Boutique activerBoutique(Long boutiqueId, HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    Long entrepriseId = boutique.getEntreprise().getId();

    boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.ACTIVER_BOUTIQUE);

    if (!isAdmin && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour activer cette boutique !");
    }

    boutique.setActif(true);
    boutique.setLastUpdated(LocalDateTime.now());

    return boutiqueRepository.save(boutique);
}

// Méthode pour supprimer une boutique
    @Transactional
    public ResponseEntity<Map<String, String>> supprimerBoutique(Long boutiqueId, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        Long entrepriseId = boutique.getEntreprise().getId();

        boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseId);
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

        if (!isAdmin && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour supprimer cette boutique !");
        }

            List<Produit> produits = produitRepository.findByBoutiqueIdAndDeletedFalse(boutiqueId);

            if (boutique.getTypeBoutique() == TypeBoutique.ENTREPOT && !produits.isEmpty()) {
                throw new RuntimeException("Impossible de supprimer cet entrepôt : il contient des produits.");
            }


            boolean tousProduitsSansStock = produits.stream()
                .allMatch(p -> !Boolean.TRUE.equals(p.getEnStock()));

            if (!produits.isEmpty() && !tousProduitsSansStock) {
                throw new RuntimeException("Impossible de supprimer cette boutique : elle contient des produits en stock.");
            }

            boolean produitLieALigneFacture = produits.stream()
                .anyMatch(produit -> ligneFactureProformaRepository.existsByProduitIdAndEntrepriseId(
                        produit.getId(), entrepriseId));

            if (produitLieALigneFacture) {
                throw new RuntimeException("Impossible de supprimer la boutique : certains produits sont liés à des factures.");
            }


        boutiqueRepository.deleteById(boutiqueId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Boutique supprimée avec succès.");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    /**
     * Liste des transferts pour l'entreprise de l'utilisateur connecté (isolation multi-tenant).
     * Si boutiqueId est fourni, seuls les transferts impliquant cette boutique sont retournés
     * (la boutique doit appartenir à l'entreprise de l'utilisateur).
     */
    public List<TransfertDTO> getTransferts(HttpServletRequest request, Long boutiqueId) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        if (user.getEntreprise() == null) {
            throw new RuntimeException("Utilisateur non rattaché à une entreprise.");
        }
        Long entrepriseId = user.getEntreprise().getId();

        List<Transfert> transferts;
        if (boutiqueId != null) {
            boutiqueRepository.findByIdAndEntrepriseId(boutiqueId, entrepriseId)
                    .orElseThrow(() -> new RuntimeException("Boutique introuvable ou n'appartient pas à votre entreprise."));
            transferts = transfertRepository.findByBoutiqueSourceIdOrBoutiqueDestinationId(boutiqueId, boutiqueId);
        } else {
            transferts = transfertRepository.findByBoutiqueSource_Entreprise_IdOrBoutiqueDestination_Entreprise_Id(entrepriseId, entrepriseId);
        }

        return transferts.stream().map(t -> {
            TransfertDTO dto = new TransfertDTO();
            dto.setId(t.getId());
            dto.setProduitNom(t.getProduit() != null ? t.getProduit().getNom() : null);
            dto.setProduitCodeGenerique(t.getProduit() != null ? t.getProduit().getCodeGenerique() : null);
            dto.setBoutiqueSourceNom(t.getBoutiqueSource() != null ? t.getBoutiqueSource().getNomBoutique() : null);
            dto.setBoutiqueDestinationNom(t.getBoutiqueDestination() != null ? t.getBoutiqueDestination().getNomBoutique() : null);
            dto.setQuantite(t.getQuantite());
            dto.setDateTransfert(t.getDateTransfert() != null ? t.getDateTransfert().toString() : null);
            return dto;
        }).collect(Collectors.toList());
    }

}

package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.StockHistory;
import com.xpertcash.entity.Transfert;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.exceptions.BusinessException;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockHistoryRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.TransfertRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class BoutiqueService {

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private JwtUtil jwtUtil; 

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private TransfertRepository transfertRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private StockHistoryRepository stockHistoryRepository;


    // Ajouter une nouvelle boutique pour l'admin
    @Transactional
    public Boutique ajouterBoutique(HttpServletRequest request, String nomBoutique, String adresse, String Telephone, String email) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'admin depuis le token
        Long adminId = null;
        try {
            adminId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'admin depuis le token", e);
        }
           // Récupérer l'admin par son ID
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // Vérifier que l'admin est bien un Admin
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut ajouter une boutique !");
        }

        // Vérifier que l'admin possède une entreprise
        if (admin.getEntreprise() == null) {
            throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
        }

        // Créer une nouvelle boutique pour l'entreprise de l'admin
        Boutique boutique = new Boutique();
        boutique.setNomBoutique(nomBoutique);
        boutique.setAdresse(adresse);
        boutique.setTelephone(Telephone);
        boutique.setEmail(email);
        boutique.setEntreprise(admin.getEntreprise());
        boutique.setCreatedAt(LocalDateTime.now());

        // Sauvegarder la boutique en base de données
        return boutiqueRepository.save(boutique);
    }


    // Récupérer toutes les boutiques d'une entreprise
    public List<Boutique> getBoutiquesByEntreprise(HttpServletRequest request) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'utilisateur depuis le token
        Long userId = null;
        try {
            userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
        }

        // Récupérer l'utilisateur par son ID
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier que l'utilisateur est bien un admin et qu'il a une entreprise associée
        if (user.getRole() == null || !user.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut récupérer les boutiques d'une entreprise !");
        }

        if (user.getEntreprise() == null) {
            throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
        }

        // Récupérer et retourner toutes les boutiques de l'entreprise
        return boutiqueRepository.findByEntrepriseId(user.getEntreprise().getId());
    }

    // Methode pour recuperer une boutique par son ID
    public Boutique getBoutiqueById(Long boutiqueId, HttpServletRequest request) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'utilisateur depuis le token
        Long userId = jwtUtil.extractUserId(token.substring(7));

        // Récupérer l'utilisateur (admin)
        User admin = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // Vérifier que l'utilisateur est bien un admin
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut accéder aux boutiques !");
        }

        // Récupérer la boutique par son ID
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        // Vérifier que la boutique appartient à l'entreprise de l'admin
        if (!boutique.getEntreprise().equals(admin.getEntreprise())) {
            throw new RuntimeException("Vous n'avez pas accès à cette boutique !");
        }

        return boutique;
    }


    //Methode update de Boutique
    public Boutique updateBoutique(Long boutiqueId, String newNomBoutique, String newAdresse,String newTelephone, String newEmail, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
    
        Long adminId = jwtUtil.extractUserId(token.substring(7));
        System.out.println("ID ADMIN EXTRAIT : " + adminId);
    
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));
    
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un ADMIN peut modifier une boutique !");
        }
    
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));
    
        // Modifier les informations de la boutique
        if (newNomBoutique != null) boutique.setNomBoutique(newNomBoutique);
        if (newAdresse != null) boutique.setAdresse(newAdresse);
        if (newTelephone != null) boutique.setTelephone(newTelephone);
        if (newEmail != null) boutique.setEmail(newEmail);
        boutique.setLastUpdated(LocalDateTime.now());
    
        return boutiqueRepository.save(boutique);
    }

    //Methode pour Transfert
    @Transactional
    public void transfererProduits(HttpServletRequest request, Long boutiqueSourceId, Long boutiqueDestinationId, Long produitId, int quantite) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long adminId = jwtUtil.extractUserId(token.substring(7));
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

         // Verication et Permission
        RoleType role = admin.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour effectuer les trnasferts !");
        }

        Boutique boutiqueSource = boutiqueRepository.findById(boutiqueSourceId)
                .orElseThrow(() -> new RuntimeException("Boutique source non trouvée"));
        Boutique boutiqueDestination = boutiqueRepository.findById(boutiqueDestinationId)
                .orElseThrow(() -> new RuntimeException("Boutique destination non trouvée"));

        if (!boutiqueSource.isActif() || !boutiqueDestination.isActif()) {
            throw new RuntimeException("L'une des boutiques est désactivée !");
        }

        if (!boutiqueSource.getEntreprise().equals(admin.getEntreprise()) ||
            !boutiqueDestination.getEntreprise().equals(admin.getEntreprise())) {
            throw new RuntimeException("Les boutiques doivent appartenir à l'entreprise de l'admin !");
        }

        Produit produit = produitRepository.findByBoutiqueAndId(boutiqueSourceId, produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé dans la boutique source"));

        if (produit.getQuantite() < quantite) {
            throw new RuntimeException("Quantité insuffisante dans la boutique source !");
        }

        produit.setQuantite(produit.getQuantite() - quantite);
        produitRepository.save(produit);

        Optional<Produit> produitDestinationOpt = produitRepository.findByBoutiqueAndCodeGenerique(boutiqueDestination.getId(), produit.getCodeGenerique());

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

        produitDestination.setEnStock(true); // 🔥 Important
        produitRepository.save(produitDestination);

        // 🔧 Stock
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

        // 📝 Historique
        StockHistory history = new StockHistory();
        history.setAction("Transfert depuis boutique " + boutiqueSource.getNomBoutique());
        history.setQuantite(quantite);
        history.setStockAvant(stock.getStockApres() - quantite);
        history.setStockApres(stock.getStockApres());
        history.setDescription("Transfert automatique via fonctionnalité de transfert");
        history.setCreatedAt(LocalDateTime.now());
        history.setStock(stock);
        history.setUser(admin);
        stockHistoryRepository.save(history);

        // 🔄 Transfert
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

        Long adminId = jwtUtil.extractUserId(token.substring(7));
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        RoleType role = admin.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour copier des produits !");
        }

        Boutique boutiqueSource = boutiqueRepository.findById(boutiqueSourceId)
                .orElseThrow(() -> new RuntimeException("Boutique source non trouvée"));
        Boutique boutiqueDestination = boutiqueRepository.findById(boutiqueDestinationId)
                .orElseThrow(() -> new RuntimeException("Boutique destination non trouvée"));

        if (!boutiqueSource.isActif()) {
            throw new RuntimeException("La boutique source est désactivée !");
        }
        if (!boutiqueDestination.isActif()) {
            throw new RuntimeException("La boutique destination est désactivée !");
        }

        if (!boutiqueSource.getEntreprise().equals(admin.getEntreprise()) ||
                !boutiqueDestination.getEntreprise().equals(admin.getEntreprise())) {
            throw new RuntimeException("Les boutiques doivent appartenir à la même entreprise !");
        }

        List<Produit> produitsACopier;

        if (toutCopier) {
            produitsACopier = produitRepository.findByBoutique(boutiqueSource);
        } else {
            if (listeProduitIds == null || listeProduitIds.isEmpty()) {
                throw new RuntimeException("Liste de produits vide !");
            }
            produitsACopier = produitRepository.findByBoutiqueAndIdIn(boutiqueSourceId, listeProduitIds);
        }

        if (produitsACopier == null || produitsACopier.isEmpty()) {
            throw new RuntimeException("Aucun produit à copier depuis la boutique source !");
        }

        int compteurProduitsCopies = 0;

        for (Produit produit : produitsACopier) {
            Optional<Produit> produitDestinationOpt = produitRepository.findByBoutiqueAndCodeGenerique(boutiqueDestination.getId(), produit.getCodeGenerique());

            if (produitDestinationOpt.isPresent()) {
                continue;
            }

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
            nouveauProduit.setEnStock(false);
            nouveauProduit.setBoutique(boutiqueDestination);

            produitRepository.save(nouveauProduit);

            Stock stockSource = stockRepository.findByProduit(produit);

            Stock stockDestination = new Stock();
            stockDestination.setProduit(nouveauProduit);

            if (stockSource != null) {
                stockDestination.setStockActuel(stockSource.getStockActuel());
                stockDestination.setQuantiteAjoute(stockSource.getStockActuel());
                stockDestination.setStockApres(stockSource.getStockActuel());
                nouveauProduit.setQuantite(stockSource.getStockActuel());
                nouveauProduit.setEnStock(stockSource.getStockActuel() > 0);
            } else {
                stockDestination.setStockActuel(0);
                stockDestination.setQuantiteAjoute(0);
                stockDestination.setStockApres(0);
            }

            stockDestination.setLastUpdated(LocalDateTime.now());

            produitRepository.save(nouveauProduit);
            stockRepository.save(stockDestination);

            compteurProduitsCopies++;
        }

        if (compteurProduitsCopies == 0) {
            throw new RuntimeException("Aucun produit n'a été copié : les produits sélectionner existent déjà dans la boutique destination.");
        }

        return compteurProduitsCopies;
    }

    public List<Produit> getProduitsParBoutique(HttpServletRequest request, Long boutiqueId) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'utilisateur depuis le token
        Long userId = jwtUtil.extractUserId(token.substring(7));

        // Récupérer l'utilisateur (admin)
        User admin = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // Vérifier que l'utilisateur est bien un admin
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut accéder aux produits !");
        }

        // Récupérer la boutique par son ID
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));
        
        // Vérifier si la boutique est active
        if (!boutique.isActif()) {
            throw new RuntimeException("Cette boutique est désactivée, ses produits ne sont pas accessibles !");
        }

        // Vérifier que la boutique appartient à l'entreprise de l'admin
        if (!boutique.getEntreprise().equals(admin.getEntreprise())) {
            throw new RuntimeException("Vous n'avez pas accès à cette boutique !");
        }

        // Retourner la liste des produits de la boutique
        return boutique.getProduits();
    }


    // Methode pour descativer une boutique
    public Boutique desactiverBoutique(Long boutiqueId, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long adminId = jwtUtil.extractUserId(token.substring(7));
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un ADMIN peut désactiver une boutique !");
        }

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        boutique.setActif(false);
        return boutiqueRepository.save(boutique);
    }
    
    // Methode pour activer une boutique
    public Boutique activerBoutique(Long boutiqueId, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long adminId = jwtUtil.extractUserId(token.substring(7));
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un ADMIN peut activer une boutique !");
        }

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        boutique.setActif(true);
        return boutiqueRepository.save(boutique);
    }
    
    //Methode pour recuperer les vendeur de la boutique
    public List<User> getVendeursByBoutique(Long boutiqueId, HttpServletRequest request) {

    // Sécurisation : récupérer le token
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }
    token = token.replace("Bearer ", "");

    // Extraire l'ID de l'admin depuis le token
    Long adminId = jwtUtil.extractUserId(token);

    // Vérifier que l'utilisateur est un admin
    User admin = usersRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

    if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
        throw new RuntimeException("Seul un ADMIN peut consulter cette liste !");
    }

    // Vérifier que la boutique appartient à l'entreprise de l'admin
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new BusinessException("Boutique introuvable."));

    if (!boutique.getEntreprise().getId().equals(admin.getEntreprise().getId())) {
        throw new BusinessException("Cette boutique ne vous appartient pas.");
    }

    List<User> vendeurs = usersRepository.findByBoutiqueIdAndRole_Name(boutiqueId, RoleType.VENDEUR);

    if (vendeurs.isEmpty()) {
        throw new BusinessException("Aucun vendeur n'est assigné à cette boutique pour le moment.");
    }

    return vendeurs;
}

}

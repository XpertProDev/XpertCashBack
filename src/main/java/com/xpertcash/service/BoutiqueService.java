package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.StockHistory;
import com.xpertcash.entity.Transfert;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.TypeBoutique;
import com.xpertcash.exceptions.BusinessException;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.LigneFactureProformaRepository;
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

    @Autowired
    private LigneFactureProformaRepository ligneFactureProformaRepository;


    // Ajouter une nouvelle boutique pour l'admin
    @Transactional
    public Boutique ajouterBoutique(HttpServletRequest request, String nomBoutique, String adresse, String Telephone, String email, TypeBoutique typeBoutique) {
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
        boutique.setTypeBoutique(typeBoutique);

        // Sauvegarder la boutique en base de données
        return boutiqueRepository.save(boutique);
    }

    // Récupérer toutes les boutiques d'une entreprise
    public List<Boutique> getBoutiquesByEntreprise(HttpServletRequest request) {
    // ✅ Vérification du token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    String jwtToken = token.substring(7); // Retirer "Bearer "

    Long userId;
    try {
        userId = jwtUtil.extractUserId(jwtToken);
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
    }

    // 👤 Récupération de l'utilisateur
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    if (user.getEntreprise() == null) {
        throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
    }

    Long entrepriseId = user.getEntreprise().getId();

    // 🔐 Vérification des rôles et permissions via CentralAccess
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour consulter les boutiques de cette entreprise !");
    }

    // ✅ Récupération des boutiques de l'entreprise
    return boutiqueRepository.findByEntrepriseId(entrepriseId);
}

    // Methode pour recuperer une boutique par son ID
    public Boutique getBoutiqueById(Long boutiqueId, HttpServletRequest request) {
    // 🔐 Vérification du token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    String jwtToken = token.substring(7); // Supprimer "Bearer "
    Long userId;

    try {
        userId = jwtUtil.extractUserId(jwtToken);
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
    }

    // 👤 Récupération de l'utilisateur
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // 🏬 Récupération de la boutique
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    Long entrepriseId = boutique.getEntreprise().getId();

    // 🔐 Vérification de l'appartenance et des droits
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour accéder à cette boutique.");
    }

    // ✅ Vérifie l'appartenance à la même entreprise
    if (!user.getEntreprise().getId().equals(entrepriseId)) {
        throw new RuntimeException("Vous n'avez pas accès à cette boutique (entreprise différente).");
    }

    return boutique;
}

    //Methode update de Boutique
    public Boutique updateBoutique(Long boutiqueId, String newNomBoutique, String newAdresse, String newTelephone, String newEmail, HttpServletRequest request) {
    // 🔐 Vérification du token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = jwtUtil.extractUserId(token.substring(7));
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // 🏬 Récupérer la boutique
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    Long entrepriseId = boutique.getEntreprise().getId();

    // 🔐 Vérification des autorisations
    boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

    if (!isAdmin && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour modifier cette boutique !");
    }

    // ♻️ Mise à jour des champs
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

        // 🔐 Vérification JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long adminId = jwtUtil.extractUserId(token.substring(7));
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // 🔑 Vérification des droits
        RoleType role = admin.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour effectuer les transferts !");
        }

        // ✅ Vérification des boutiques
        Boutique boutiqueSource = boutiqueRepository.findById(boutiqueSourceId)
                .orElseThrow(() -> new RuntimeException("Boutique source non trouvée"));
        Boutique boutiqueDestination = boutiqueRepository.findById(boutiqueDestinationId)
                .orElseThrow(() -> new RuntimeException("Boutique destination non trouvée"));

        if (!boutiqueSource.isActif() || !boutiqueDestination.isActif()) {
            throw new RuntimeException("L'une des boutiques est désactivée !");
        }

        if (!boutiqueSource.getEntreprise().equals(admin.getEntreprise()) ||
            !boutiqueDestination.getEntreprise().equals(admin.getEntreprise())) {
            throw new RuntimeException("Les boutiques doivent appartenir à l'entreprise de l'utilisateur !");
        }

        // 🔍 Vérification du produit source
        Produit produit = produitRepository.findByBoutiqueAndId(boutiqueSourceId, produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé dans la boutique source"));

        if (produit.getQuantite() < quantite) {
            throw new RuntimeException("Quantité insuffisante dans la boutique source !");
        }

        // 🔽 Mise à jour de la quantité dans la boutique source
        produit.setQuantite(produit.getQuantite() - quantite);
        produitRepository.save(produit);

        // 📦 Vérification ou création du produit dans la boutique de destination
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

        // 🔄 Mise à jour ou création du stock
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

        // 📝 Historique de stock
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

        // 💾 Enregistrement du transfert
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
        // 🔐 Vérifier la présence et le format du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long userId = jwtUtil.extractUserId(token.substring(7));
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 🏬 Récupération des boutiques
        Boutique boutiqueSource = boutiqueRepository.findById(boutiqueSourceId)
                .orElseThrow(() -> new RuntimeException("Boutique source non trouvée"));
        Boutique boutiqueDestination = boutiqueRepository.findById(boutiqueDestinationId)
                .orElseThrow(() -> new RuntimeException("Boutique destination non trouvée"));

        Long entrepriseId = boutiqueSource.getEntreprise().getId();

        // 🔐 Vérification des droits via CentralAccess
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId);
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour copier des produits !");
        }

        // ✅ Vérifier l'appartenance des deux boutiques à la même entreprise
        if (!boutiqueSource.getEntreprise().getId().equals(entrepriseId)
            || !boutiqueDestination.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Les boutiques doivent appartenir à la même entreprise !");
        }

        // ❌ Vérifier que les deux boutiques sont actives
        if (!boutiqueSource.isActif() || !boutiqueDestination.isActif()) {
            throw new RuntimeException("Les boutiques doivent être actives !");
        }

        // 🛒 Produits à copier
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

            // 📦 Stock initialisé à zéro
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

   
    public List<Produit> getProduitsParBoutique(HttpServletRequest request, Long boutiqueId) {
        // 🔐 Vérifier la présence et le format du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        String jwtToken = token.substring(7);
        Long userId;
        try {
            userId = jwtUtil.extractUserId(jwtToken);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
        }

        // 👤 Récupérer l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 🏬 Récupérer la boutique
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        Long entrepriseId = boutique.getEntreprise().getId();

        // 🔒 Vérifier les droits d'accès avec CentralAccess
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId);
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour accéder aux produits de cette boutique.");
        }

        // ✅ Vérifier l'appartenance à la même entreprise
        if (!user.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Vous n'avez pas accès à cette boutique (entreprise différente).");
        }

        // ❌ Vérifier si la boutique est désactivée
        if (!boutique.isActif()) {
            throw new RuntimeException("Cette boutique est désactivée, ses produits ne sont pas accessibles !");
        }

        // ✅ Retourner les produits non supprimés via le repository
        return produitRepository.findByBoutiqueIdAndNotDeleted(boutiqueId);
    }
 
    // Methode pour descativer une boutique
     public Boutique desactiverBoutique(Long boutiqueId, HttpServletRequest request) {
    // 🔐 Vérification du token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = jwtUtil.extractUserId(token.substring(7));
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // 🏬 Récupération de la boutique
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    Long entrepriseId = boutique.getEntreprise().getId();

    // 🔐 Vérification des droits
    boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.DESACTIVER_BOUTIQUE);

    if (!isAdmin && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour désactiver cette boutique !");
    }

    // 🚫 Désactivation
    boutique.setActif(false);
    boutique.setLastUpdated(LocalDateTime.now());

    return boutiqueRepository.save(boutique);
}

    // Methode pour activer une boutique
    public Boutique activerBoutique(Long boutiqueId, HttpServletRequest request) {
    // 🔐 Vérification du token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = jwtUtil.extractUserId(token.substring(7));
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // 🏬 Récupération de la boutique
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    Long entrepriseId = boutique.getEntreprise().getId();

    // 🔐 Vérification des droits
    boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.ACTIVER_BOUTIQUE);

    if (!isAdmin && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour activer cette boutique !");
    }

    // ✅ Activation
    boutique.setActif(true);
    boutique.setLastUpdated(LocalDateTime.now());

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

// Méthode pour supprimer une boutique
    @Transactional
    public ResponseEntity<Map<String, String>> supprimerBoutique(Long boutiqueId, HttpServletRequest request) {
        // 🔐 Vérification du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long userId = jwtUtil.extractUserId(token.substring(7));
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 🏬 Récupération de la boutique
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        Long entrepriseId = boutique.getEntreprise().getId();

        // 🔐 Vérification des droits
        boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entrepriseId);
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

        if (!isAdmin && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour supprimer cette boutique !");
        }

        // 📦 Vérification des produits
            List<Produit> produits = produitRepository.findByBoutiqueIdAndDeletedFalse(boutiqueId);

            // ⚠️ Cas spécial : ne pas supprimer un entrepôt s’il contient des produits (peu importe le stock)
            if (boutique.getTypeBoutique() == TypeBoutique.ENTREPOT && !produits.isEmpty()) {
                throw new RuntimeException("Impossible de supprimer cet entrepôt : il contient des produits.");
            }


            // ✅ Cas normal pour les autres boutiques : ne pas supprimer s’il reste des produits en stock
            boolean tousProduitsSansStock = produits.stream()
                .allMatch(p -> !Boolean.TRUE.equals(p.getEnStock()));

            if (!produits.isEmpty() && !tousProduitsSansStock) {
                throw new RuntimeException("Impossible de supprimer cette boutique : elle contient des produits en stock.");
            }

            // Vérification si un produit est lié à une ligne de facture
            boolean produitLieALigneFacture = produits.stream()
                .anyMatch(produit -> ligneFactureProformaRepository.existsByProduitId(produit.getId()));

            if (produitLieALigneFacture) {
                throw new RuntimeException("Impossible de supprimer la boutique : certains produits sont liés à des factures.");
            }


        // 🗑️ Suppression
        boutiqueRepository.deleteById(boutiqueId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Boutique supprimée avec succès.");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }


}

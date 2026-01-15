package com.xpertcash.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.xpertcash.DTOs.SuperAdminEntrepriseListDTO;
import com.xpertcash.DTOs.SuperAdminEntrepriseStatsDTO;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.VENTE.StatutCaisse;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Client;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Unite;
import com.xpertcash.entity.DepenseGenerale;
import com.xpertcash.entity.EntreeGenerale;
import com.xpertcash.entity.CategorieDepense;
import com.xpertcash.entity.TransfertFonds;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.UserBoutique;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.GlobalNotification;
import com.xpertcash.entity.PROSPECT.Prospect;
import com.xpertcash.entity.Module.PaiementModule;
import com.xpertcash.entity.Module.EntrepriseModuleEssai;
import com.xpertcash.entity.Module.EntrepriseModuleAbonnement;
import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.VENTE.VersementComptable;
import com.xpertcash.entity.Caisse;
import com.xpertcash.entity.VENTE.MouvementCaisse;
import com.xpertcash.entity.FactureVente;
import com.xpertcash.entity.VENTE.VenteHistorique;
import com.xpertcash.entity.NoteFactureProForma;
import com.xpertcash.entity.PROSPECT.Interaction;
import com.xpertcash.entity.PROSPECT.ProspectAchat;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.UniteRepository;
import com.xpertcash.repository.DepenseGeneraleRepository;
import com.xpertcash.repository.EntreeGeneraleRepository;
import com.xpertcash.repository.CategorieDepenseRepository;
import com.xpertcash.repository.TransfertFondsRepository;
import com.xpertcash.repository.PROSPECT.ProspectRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.StockHistoryRepository;
import com.xpertcash.repository.StockProduitFournisseurRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.UserSessionRepository;
import com.xpertcash.repository.VENTE.CaisseRepository;
import com.xpertcash.repository.VENTE.VenteRepository;
import com.xpertcash.repository.VENTE.VersementComptableRepository;
import com.xpertcash.repository.VENTE.MouvementCaisseRepository;
import com.xpertcash.repository.FactureVenteRepository;
import com.xpertcash.repository.VENTE.VenteHistoriqueRepository;
import com.xpertcash.repository.NoteFactureProFormaRepository;
import com.xpertcash.repository.PROSPECT.InteractionRepository;
import com.xpertcash.repository.PROSPECT.ProspectAchatRepository;
import com.xpertcash.repository.FactProHistoriqueActionRepository;
import com.xpertcash.repository.PASSWORD.PasswordResetTokenRepository;
import com.xpertcash.repository.PASSWORD.InitialPasswordTokenRepository;
import com.xpertcash.repository.Module.PaiementModuleRepository;
import com.xpertcash.repository.Module.EntrepriseModuleEssaiRepository;
import com.xpertcash.repository.Module.EntrepriseModuleAbonnementRepository;
import com.xpertcash.repository.GlobalNotificationRepository;
import com.xpertcash.repository.UserBoutiqueRepository;
import com.xpertcash.repository.FactureRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service réservé aux opérations du SUPER_ADMIN (propriétaire de la plateforme).
 */
@Service
public class SuperAdminService {

    @Autowired
    private EntrepriseService entrepriseService;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private StockProduitFournisseurRepository stockProduitFournisseurRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    private FactureReelleRepository factureReelleRepository;

    @Autowired
    private CaisseRepository caisseRepository;

    @Autowired
    private VenteRepository venteRepository;

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private UniteRepository uniteRepository;

    @Autowired
    private DepenseGeneraleRepository depenseGeneraleRepository;

    @Autowired
    private EntreeGeneraleRepository entreeGeneraleRepository;

    @Autowired
    private CategorieDepenseRepository categorieDepenseRepository;

    @Autowired
    private TransfertFondsRepository transfertFondsRepository;

    @Autowired
    private PaiementModuleRepository paiementModuleRepository;

    @Autowired
    private EntrepriseModuleEssaiRepository entrepriseModuleEssaiRepository;

    @Autowired
    private EntrepriseModuleAbonnementRepository entrepriseModuleAbonnementRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private GlobalNotificationRepository globalNotificationRepository;

    @Autowired
    private UserBoutiqueRepository userBoutiqueRepository;

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private MouvementCaisseRepository mouvementCaisseRepository;

    @Autowired
    private VersementComptableRepository versementComptableRepository;

    @Autowired
    private FactureVenteRepository factureVenteRepository;

    @Autowired
    private VenteHistoriqueRepository venteHistoriqueRepository;

    @Autowired
    private NoteFactureProFormaRepository noteFactureProFormaRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ProspectAchatRepository prospectAchatRepository;

    @Autowired
    private FactProHistoriqueActionRepository factProHistoriqueActionRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private InitialPasswordTokenRepository initialPasswordTokenRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Vérifie que l'utilisateur est SUPER_ADMIN, sinon lève une exception.
     */
    public void ensureSuperAdmin(User user) {
        if (user == null || user.getRole() == null || user.getRole().getName() != RoleType.SUPER_ADMIN) {
            throw new RuntimeException("Accès refusé : réservé au SUPER_ADMIN.");
        }
    }

    /**
     * Récupère la liste paginée des entreprises avec leurs infos (vue globale plateforme)
     * pour la vue SUPER_ADMIN.
     */
    public Page<SuperAdminEntrepriseListDTO> getAllEntreprisesAsSuperAdmin(User superAdmin, int page, int size) {
        ensureSuperAdmin(superAdmin);

        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        Pageable pageable = PageRequest.of(page, size);

        // Exclure l'entreprise technique du SUPER_ADMIN de la liste
        String superAdminEntrepriseName = "XpertCash Super Admin";
        Page<Entreprise> entreprisesPage = entrepriseRepository.findAllExcludingNomEntreprise(superAdminEntrepriseName, pageable);

        return entreprisesPage.map(entreprise -> {
            String adminId = entreprise.getAdmin() != null ? entreprise.getAdmin().getId().toString() : null;
            String adminNom = entreprise.getAdmin() != null ? entreprise.getAdmin().getNomComplet() : "Aucun Admin";
            String adminPhone = entreprise.getAdmin() != null ? entreprise.getAdmin().getPhone() : null;
            String adminEmail = entreprise.getAdmin() != null ? entreprise.getAdmin().getEmail() : null;

            long nombreUtilisateursEntreprise = usersRepository.countByEntrepriseId(entreprise.getId());

            return new SuperAdminEntrepriseListDTO(
                    entreprise.getId(),
                    entreprise.getNomEntreprise(),
                    entreprise.getCreatedAt(),
                    entreprise.getPays(),
                    entreprise.getSecteur(),
                    adminId,
                    adminNom,
                    adminPhone,
                    adminEmail,
                    nombreUtilisateursEntreprise
            );
        });
    }

    /**
     * Désactiver une entreprise (réservé SUPER_ADMIN).
     */
    public void desactiverEntreprise(User superAdmin, Long entrepriseId) {
        ensureSuperAdmin(superAdmin);
        Entreprise entreprise = entrepriseService.getEntrepriseById(entrepriseId);
        entreprise.setActive(false);
        entrepriseRepository.save(entreprise);
    }

    /**
     * Réactiver une entreprise (réservé SUPER_ADMIN).
     */
    public void activerEntreprise(User superAdmin, Long entrepriseId) {
        ensureSuperAdmin(superAdmin);
        Entreprise entreprise = entrepriseService.getEntrepriseById(entrepriseId);
        entreprise.setActive(true);
        entrepriseRepository.save(entreprise);
    }

    /**
     * Récupérer une entreprise par son id avec toutes les statistiques demandées
     * (réservé au SUPER_ADMIN).
     */
    public SuperAdminEntrepriseStatsDTO getEntrepriseStats(User superAdmin, Long entrepriseId) {
        ensureSuperAdmin(superAdmin);

        Entreprise entreprise = entrepriseService.getEntrepriseById(entrepriseId);

        Long id = entreprise.getId();

        // Utilisateurs (tous ceux de l'entreprise)
        long nombreUtilisateurs = usersRepository.countByEntrepriseId(id);

        // Boutiques
        long nombreBoutiques = boutiqueRepository.countByEntrepriseId(id);

        // Produits (références uniques, déjà optimisé via COUNT DISTINCT)
        long nombreProduits = produitRepository.countProduitsUniquesByEntrepriseId(id);

        // Stocks (nombre d'enregistrements de stock pour les boutiques de l'entreprise)
        long nombreStocks = stockRepository.countByEntrepriseId(id);

        // Clients
        long particuliers = clientRepository.countClientsDirectByEntrepriseId(id);
        long entreprisesClients = clientRepository.countClientsEntrepriseByEntrepriseId(id);
        long totalClients = particuliers + entreprisesClients;

        // Prospects
        long nombreProspects = prospectRepository.countByEntrepriseId(id);

        // Factures
        long nombreFacturesProforma = factureProformaRepository.countFacturesByEntrepriseId(id);
        long nombreFacturesReelles = factureReelleRepository.countByEntrepriseId(id);

        // Caisses ouvertes
        long nombreCaissesOuvertes = caisseRepository.countByEntrepriseIdAndStatut(id, StatutCaisse.OUVERTE);

        // Ventes
        long nombreVentes = venteRepository.countByEntrepriseId(id);

        SuperAdminEntrepriseStatsDTO dto = new SuperAdminEntrepriseStatsDTO();
        dto.setEntrepriseId(id);
        dto.setNomEntreprise(entreprise.getNomEntreprise());
        dto.setActive(Boolean.TRUE.equals(entreprise.getActive()));
        dto.setCreatedAt(entreprise.getCreatedAt());

        dto.setNombreUtilisateurs(nombreUtilisateurs);
        dto.setNombreBoutiques(nombreBoutiques);
        dto.setNombreProduits(nombreProduits);
        dto.setNombreStocks(nombreStocks);

        dto.setNombreClientsTotal(totalClients);
        dto.setNombreClientsParticuliers(particuliers);
        dto.setNombreClientsEntreprises(entreprisesClients);

        dto.setNombreProspects(nombreProspects);

        dto.setNombreFacturesProforma(nombreFacturesProforma);
        dto.setNombreFacturesReelles(nombreFacturesReelles);

        dto.setNombreCaissesOuvertes(nombreCaissesOuvertes);
        dto.setNombreVentes(nombreVentes);

        return dto;
    }

    /**
     * Supprime un Admin et TOUTES les données associées à son entreprise.
     * Cette méthode supprime de manière récursive toutes les entités liées à l'entreprise.
     * 
     * Peut être appelée par :
     * - SUPER_ADMIN (peut supprimer n'importe quel admin)
     * - L'admin lui-même (peut supprimer uniquement son propre compte)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdminAndEntreprise(User user, Long adminId) {
        // Vérifier les droits : soit SUPER_ADMIN, soit l'admin lui-même
        boolean isSuperAdmin = user.getRole() != null && user.getRole().getName() == RoleType.SUPER_ADMIN;
        boolean isSelf = user.getId().equals(adminId);
        
        if (!isSuperAdmin && !isSelf) {
            throw new RuntimeException("Accès refusé : vous ne pouvez supprimer que votre propre compte ou être SUPER_ADMIN.");
        }

        // 1. Récupérer l'admin et vérifier qu'il existe et qu'il est bien un admin
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin introuvable avec l'ID : " + adminId));

        if (admin.getRole() == null || admin.getRole().getName() != RoleType.ADMIN) {
            throw new RuntimeException("L'utilisateur avec l'ID " + adminId + " n'est pas un administrateur.");
        }

        // 2. Si c'est l'admin lui-même (pas SUPER_ADMIN), vérifier qu'il supprime bien son propre compte
        if (!isSuperAdmin && isSelf) {
            // Vérifier que l'utilisateur connecté est bien un admin
            if (user.getRole() == null || user.getRole().getName() != RoleType.ADMIN) {
                throw new RuntimeException("Vous devez être administrateur pour supprimer votre compte.");
            }
            // Vérifier que l'admin appartient à la même entreprise (déjà vérifié par isSelf, mais double vérification)
            if (user.getEntreprise() == null || admin.getEntreprise() == null ||
                !user.getEntreprise().getId().equals(admin.getEntreprise().getId())) {
                throw new RuntimeException("Vous ne pouvez supprimer que votre propre compte.");
            }
        }

        // 3. Récupérer l'entreprise de l'admin
        Entreprise entreprise = admin.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'admin n'a pas d'entreprise associée.");
        }

        Long entrepriseId = entreprise.getId();

        // 3. Retirer la référence admin de l'entreprise avant de supprimer les utilisateurs
        // Cela évite l'erreur TransientObjectException
        entreprise.setAdmin(null);
        entrepriseRepository.save(entreprise);

        // 4. Supprimer toutes les dépenses générales AVANT les utilisateurs (car elles référencent creePar)
        List<DepenseGenerale> depenses = depenseGeneraleRepository.findByEntrepriseId(entrepriseId);
        for (DepenseGenerale depense : depenses) {
            // Supprimer la pièce jointe de la dépense
            if (depense.getPieceJointe() != null && !depense.getPieceJointe().isBlank()) {
                try {
                    Path pieceJointePath = Paths.get("src/main/resources/static" + depense.getPieceJointe());
                    Files.deleteIfExists(pieceJointePath);
                } catch (IOException e) {
                    // Erreur silencieuse lors de la suppression de la pièce jointe
                }
            }
        }
        depenseGeneraleRepository.deleteAll(depenses);

        // 5. Supprimer toutes les entrées générales AVANT les utilisateurs (car elles référencent creePar et responsable)
        List<EntreeGenerale> entrees = entreeGeneraleRepository.findByEntrepriseId(entrepriseId);
        for (EntreeGenerale entree : entrees) {
            // Supprimer la pièce jointe de l'entrée
            if (entree.getPieceJointe() != null && !entree.getPieceJointe().isBlank()) {
                try {
                    Path pieceJointePath = Paths.get("src/main/resources/static" + entree.getPieceJointe());
                    Files.deleteIfExists(pieceJointePath);
                } catch (IOException e) {
                    // Erreur silencieuse lors de la suppression de la pièce jointe
                }
            }
        }
        entreeGeneraleRepository.deleteAll(entrees);

        // 5.5. Supprimer tous les transferts de fonds AVANT les utilisateurs (car ils référencent creePar)
        List<TransfertFonds> transferts = transfertFondsRepository.findByEntrepriseIdOrderByDateTransfertDesc(entrepriseId);
        transfertFondsRepository.deleteAll(transferts);

        // 5.6. Supprimer toutes les notifications globales AVANT les utilisateurs (car elles référencent recipient)
        List<User> usersForNotifications = usersRepository.findByEntrepriseId(entrepriseId);
        for (User u : usersForNotifications) {
            List<GlobalNotification> notifications = globalNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(u.getId());
            if (!notifications.isEmpty()) {
                globalNotificationRepository.deleteAll(notifications);
            }
        }

        // 5.6.5. Supprimer tous les FactureProduit AVANT les factures (car ils référencent facture qui référence user)
        List<Boutique> boutiquesForFactureProduit = boutiqueRepository.findByEntrepriseId(entrepriseId);
        int totalFactureProduits = 0;
        for (Boutique boutique : boutiquesForFactureProduit) {
            List<Facture> facturesBoutique = factureRepository.findByBoutiqueIdAndEntrepriseId(
                    boutique.getId(), entrepriseId);
            for (Facture facture : facturesBoutique) {
                // FactureProduit est supprimé en cascade avec Facture, mais on le fait explicitement pour être sûr
                // Utiliser une requête native pour supprimer directement
                int deleted = entityManager.createNativeQuery(
                    "DELETE FROM facture_produit WHERE facture_id = :factureId"
                ).setParameter("factureId", facture.getId()).executeUpdate();
                totalFactureProduits += deleted;
            }
        }
        if (totalFactureProduits > 0) {
            entityManager.flush();
        }

        // 5.7. Supprimer toutes les factures (liées aux boutiques) AVANT les utilisateurs (car elles référencent user)
        List<Facture> factures = factureRepository.findAllByEntrepriseId(entrepriseId);
        factureRepository.deleteAll(factures);
        entityManager.flush();

        // 5.7.5. Supprimer tous les mouvements de caisse AVANT les versements comptables (car ils référencent caisse)
        List<Boutique> boutiquesForMouvements = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForMouvements) {
            List<Caisse> caissesBoutique = caisseRepository.findByBoutiqueId(boutique.getId());
            for (Caisse caisse : caissesBoutique) {
                // Récupérer tous les mouvements de cette caisse (tous types confondus)
                List<MouvementCaisse> allMouvements = mouvementCaisseRepository.findByCaisseId(caisse.getId());
                
                if (!allMouvements.isEmpty()) {
                    mouvementCaisseRepository.deleteAll(allMouvements);
                }
            }
        }

        // 5.8. Supprimer tous les versements comptables AVANT les utilisateurs (car ils référencent creePar)
        List<Boutique> boutiquesForVersements = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForVersements) {
            List<VersementComptable> versements = versementComptableRepository.findByCaisse_BoutiqueId(boutique.getId());
            if (!versements.isEmpty()) {
                versementComptableRepository.deleteAll(versements);
            }
        }

        // 5.9. Supprimer toutes les relations UserBoutique AVANT les utilisateurs
        for (User u : usersForNotifications) {
            List<UserBoutique> userBoutiques = userBoutiqueRepository.findByUserId(u.getId());
            if (!userBoutiques.isEmpty()) {
                userBoutiqueRepository.deleteAll(userBoutiques);
            }
        }

        // 5.9.5. Supprimer tous les PasswordResetToken AVANT les utilisateurs (car ils référencent User)
        int totalPasswordTokens = 0;
        for (User u : usersForNotifications) {
            passwordResetTokenRepository.deleteByUser(u);
            totalPasswordTokens++;
        }
        if (totalPasswordTokens > 0) {
            entityManager.flush();
        }

        // 5.9.5.5. Supprimer tous les InitialPasswordToken AVANT les utilisateurs (car ils référencent User)
        int totalInitialPasswordTokens = 0;
        for (User u : usersForNotifications) {
            initialPasswordTokenRepository.deleteByUser(u);
            totalInitialPasswordTokens++;
        }
        if (totalInitialPasswordTokens > 0) {
            entityManager.flush();
        }

        // 5.9.6. Supprimer tous les paiements AVANT les utilisateurs (car ils référencent User encaissePar)
        List<FactureReelle> facturesReellesForPaiements = factureReelleRepository.findByEntrepriseId(entrepriseId);
        int totalPaiements = 0;
        for (FactureReelle factureReelle : facturesReellesForPaiements) {
            // Utiliser une requête native pour supprimer directement les paiements
            int deleted = entityManager.createNativeQuery(
                "DELETE FROM paiement WHERE facture_reelle_id = :factureReelleId"
            ).setParameter("factureReelleId", factureReelle.getId()).executeUpdate();
            totalPaiements += deleted;
        }
        if (totalPaiements > 0) {
            entityManager.flush();
        }

        // 5.9.7. Mettre à null toutes les références User dans les FactureReelle AVANT de supprimer les User
        int facturesReellesUpdated = 0;
        for (FactureReelle factureReelle : facturesReellesForPaiements) {
            boolean updated = false;
            if (factureReelle.getUtilisateurCreateur() != null) {
                factureReelle.setUtilisateurCreateur(null);
                updated = true;
            }
            if (factureReelle.getUtilisateurAnnulateur() != null) {
                factureReelle.setUtilisateurAnnulateur(null);
                updated = true;
            }
            if (factureReelle.getUtilisateurValidateur() != null) {
                factureReelle.setUtilisateurValidateur(null);
                updated = true;
            }
            if (updated) {
                factureReelleRepository.save(factureReelle);
                facturesReellesUpdated++;
            }
        }
        if (facturesReellesUpdated > 0) {
            entityManager.flush();
        }

        // 5.9.8. Mettre à null toutes les références User dans les FactureProForma AVANT de supprimer les User
        List<FactureProForma> facturesProformaForNull = factureProformaRepository.findByEntrepriseId(entrepriseId);
        int facturesProformaUpdated = 0;
        for (FactureProForma factureProforma : facturesProformaForNull) {
            boolean updated = false;
            if (factureProforma.getUtilisateurCreateur() != null) {
                factureProforma.setUtilisateurCreateur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurValidateur() != null) {
                factureProforma.setUtilisateurValidateur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurModificateur() != null) {
                factureProforma.setUtilisateurModificateur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurRelanceur() != null) {
                factureProforma.setUtilisateurRelanceur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurApprobateur() != null) {
                factureProforma.setUtilisateurApprobateur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurAnnulateur() != null) {
                factureProforma.setUtilisateurAnnulateur(null);
                updated = true;
            }
            // Vider la collection ManyToMany des approbateurs
            if (factureProforma.getApprobateurs() != null && !factureProforma.getApprobateurs().isEmpty()) {
                factureProforma.getApprobateurs().clear();
                updated = true;
            }
            if (updated) {
                factureProformaRepository.save(factureProforma);
                facturesProformaUpdated++;
            }
        }
        if (facturesProformaUpdated > 0) {
            entityManager.flush();
        }

        // 5.9.9. Supprimer toutes les NoteFactureProForma AVANT les utilisateurs (car elles référencent User auteur)
        List<FactureProForma> facturesProformaForNotes = factureProformaRepository.findByEntrepriseId(entrepriseId);
        int totalNotes = 0;
        for (FactureProForma facture : facturesProformaForNotes) {
            // Supprimer les notes de cette facture (isolé par entreprise)
            Long factureEntrepriseId = facture.getEntreprise() != null ? facture.getEntreprise().getId() : null;
            if (factureEntrepriseId != null) {
                List<NoteFactureProForma> notes = noteFactureProFormaRepository.findByFactureProFormaIdAndEntrepriseId(
                        facture.getId(), factureEntrepriseId);
                if (!notes.isEmpty()) {
                    totalNotes += notes.size();
                    noteFactureProFormaRepository.deleteAll(notes);
                }
            }
        }
        if (totalNotes > 0) {
            entityManager.flush();
        }

        // 5.9.10. Supprimer tous les FactProHistoriqueAction AVANT les utilisateurs (car ils référencent User utilisateur)
        List<FactureProForma> facturesProformaForHistoriques = factureProformaRepository.findByEntrepriseId(entrepriseId);
        int totalFactProHistoriquesActions = 0;
        for (FactureProForma facture : facturesProformaForHistoriques) {
            // Supprimer les historiques d'action de cette facture
            factProHistoriqueActionRepository.deleteByFacture(facture);
            totalFactProHistoriquesActions++;
        }
        if (totalFactProHistoriquesActions > 0) {
            entityManager.flush();
        }

        // 5.9.11. Supprimer tous les StockHistory AVANT les utilisateurs (car ils référencent User)
        // Utiliser une requête native pour supprimer directement tous les StockHistory liés à l'entreprise
        int totalStockHistories = entityManager.createNativeQuery(
            "DELETE sh FROM stock_history sh " +
            "INNER JOIN stock s ON sh.stock_id = s.id " +
            "INNER JOIN produit p ON s.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalStockHistories > 0) {
            entityManager.flush();
        }

        // 5.10. Supprimer d'abord les entités qui référencent les ventes, puis les ventes elles-mêmes
        List<Vente> ventesWithReferences = venteRepository.findAllByEntrepriseId(entrepriseId);
        
        // ÉTAPE 1 : Supprimer les historiques et factures de vente AVANT de modifier les ventes
        int totalHistoriques = 0;
        int[] totalFacturesVente = {0}; // Utiliser un tableau pour être modifiable dans la lambda
        for (Vente vente : ventesWithReferences) {
            // Supprimer les historiques de cette vente
            List<VenteHistorique> historiques = venteHistoriqueRepository.findByVenteId(vente.getId());
            if (!historiques.isEmpty()) {
                totalHistoriques += historiques.size();
                venteHistoriqueRepository.deleteAll(historiques);
            }
            
            // Supprimer les factures de vente pour cette vente spécifique (isolé par entreprise)
            Long venteEntrepriseId = vente.getBoutique() != null && vente.getBoutique().getEntreprise() != null 
                    ? vente.getBoutique().getEntreprise().getId() : null;
            if (venteEntrepriseId != null) {
                factureVenteRepository.findByVenteIdAndEntrepriseId(vente.getId(), venteEntrepriseId)
                        .ifPresent(factureVente -> {
                            factureVenteRepository.delete(factureVente);
                            totalFacturesVente[0]++;
                        });
            }
        }
        if (totalHistoriques > 0) {
        }
        if (totalFacturesVente[0] > 0) {
        }
        
        // Vérifier s'il reste des factures de vente (par sécurité, avant de modifier les ventes)
        List<FactureVente> facturesVenteRestantes = factureVenteRepository.findAllByEntrepriseId(entrepriseId);
        if (!facturesVenteRestantes.isEmpty()) {
            factureVenteRepository.deleteAll(facturesVenteRestantes);
        }
        
        // Forcer un flush pour s'assurer que toutes les FactureVente sont supprimées
        entityManager.flush();
        
        // ÉTAPE 2 : Maintenant mettre à null les références dans les ventes
        int ventesUpdated = 0;
        for (Vente vente : ventesWithReferences) {
            boolean updated = false;
            if (vente.getCaisse() != null) {
                vente.setCaisse(null);
                updated = true;
            }
            if (vente.getVendeur() != null) {
                vente.setVendeur(null);
                updated = true;
            }
            if (vente.getBoutique() != null) {
                vente.setBoutique(null);
                updated = true;
            }
            if (vente.getClient() != null) {
                vente.setClient(null);
                updated = true;
            }
            if (vente.getEntrepriseClient() != null) {
                vente.setEntrepriseClient(null);
                updated = true;
            }
            if (updated) {
                venteRepository.save(vente);
                ventesUpdated++;
            }
        }
        if (ventesUpdated > 0) {
            entityManager.flush();
        }
        
        // ÉTAPE 3 : Maintenant supprimer les ventes elles-mêmes
        if (!ventesWithReferences.isEmpty()) {
            venteRepository.deleteAll(ventesWithReferences);
            // Forcer un flush pour s'assurer que les Vente sont supprimées
            entityManager.flush();
        }

        // 5.11. Supprimer toutes les caisses AVANT les utilisateurs (car elles référencent vendeur_id)
        List<Boutique> boutiquesForCaisses = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForCaisses) {
            List<Caisse> caisses = caisseRepository.findByBoutiqueId(boutique.getId());
            if (!caisses.isEmpty()) {
                caisseRepository.deleteAll(caisses);
            }
        }

        // 5.12. Supprimer toutes les sessions des utilisateurs de cette entreprise AVANT de supprimer les utilisateurs
        // Cela évite les erreurs de contrainte de clé étrangère sur user_sessions.user_id
        entityManager.createNativeQuery(
                "DELETE us FROM user_sessions us " +
                "INNER JOIN user u ON us.user_id = u.id " +
                "WHERE u.entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        entityManager.flush();

        // 6. Supprimer tous les utilisateurs de l'entreprise (avec leurs fichiers)
        List<User> users = usersRepository.findByEntrepriseId(entrepriseId);
        for (User utilisateur : users) {
            // Supprimer le QR Code de l'utilisateur
            if (utilisateur.getQrCodeUrl() != null && !utilisateur.getQrCodeUrl().isBlank()) {
                try {
                    imageStorageService.deleteQrCodeImage(utilisateur.getQrCodeUrl());
                } catch (Exception e) {
                }
            }
            // Supprimer la photo de l'utilisateur
            if (utilisateur.getPhoto() != null && !utilisateur.getPhoto().isBlank()) {
                try {
                    Path photoPath = Paths.get("src/main/resources/static" + utilisateur.getPhoto());
                    Files.deleteIfExists(photoPath);
                } catch (IOException e) {
                }
            }
        }
        usersRepository.deleteAll(users);
        entityManager.flush();

        // 7. Supprimer toutes les factures réelles
        List<FactureReelle> facturesReelles = factureReelleRepository.findByEntrepriseId(entrepriseId);
        factureReelleRepository.deleteAll(facturesReelles);
        entityManager.flush();


        // 9. Supprimer toutes les factures proforma
        List<FactureProForma> facturesProforma = factureProformaRepository.findByEntrepriseId(entrepriseId);
        factureProformaRepository.deleteAll(facturesProforma);
        entityManager.flush();

        // 10. Supprimer tous les clients (directement liés et via EntrepriseClient) avec leurs photos
        List<Client> clients = clientRepository.findClientsByEntrepriseOrEntrepriseClient(entrepriseId);
        for (Client client : clients) {
            // Supprimer la photo du client
            if (client.getPhoto() != null && !client.getPhoto().isBlank()) {
                try {
                    Path photoPath = Paths.get("src/main/resources/static" + client.getPhoto());
                    Files.deleteIfExists(photoPath);
                } catch (IOException e) {
                }
            }
        }
        if (!clients.isEmpty()) {
            clientRepository.deleteAll(clients);
            // Forcer un flush pour s'assurer que tous les clients sont supprimés
            entityManager.flush();
        }
        
        // Vérification supplémentaire avec une requête native pour s'assurer qu'il ne reste aucun client
        // Utiliser EntityManager pour exécuter une requête native
        Long countClientsRestants = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM client WHERE entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        if (countClientsRestants > 0) {
            // Supprimer directement avec une requête native
            entityManager.createNativeQuery(
                "DELETE FROM client WHERE entreprise_id = :entrepriseId"
            ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            entityManager.flush();
        }

        // 11. Supprimer toutes les entreprises clients
        List<EntrepriseClient> entreprisesClients = entrepriseClientRepository.findByEntrepriseId(entrepriseId);
        if (!entreprisesClients.isEmpty()) {
            entrepriseClientRepository.deleteAll(entreprisesClients);
            entityManager.flush();
        }
        
        // Vérification finale : s'assurer qu'il ne reste aucun client ou entreprise client
        Long countClientsFinal = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM client WHERE entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        Long countEntreprisesClientsFinal = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM entreprise_client WHERE entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        if (countClientsFinal > 0 || countEntreprisesClientsFinal > 0) {
            if (countClientsFinal > 0) {
                entityManager.createNativeQuery(
                    "DELETE FROM client WHERE entreprise_id = :entrepriseId"
                ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            }
            if (countEntreprisesClientsFinal > 0) {
                entityManager.createNativeQuery(
                    "DELETE FROM entreprise_client WHERE entreprise_id = :entrepriseId"
                ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            }
            entityManager.flush();
        }

        // 12. Supprimer tous les fournisseurs avec leurs photos (isolé par entreprise)
        List<Fournisseur> fournisseurs = fournisseurRepository.findByEntrepriseId(entrepriseId);
        for (Fournisseur fournisseur : fournisseurs) {
            // Supprimer la photo du fournisseur
            if (fournisseur.getPhoto() != null && !fournisseur.getPhoto().isBlank()) {
                try {
                    Path photoPath = Paths.get("src/main/resources/static" + fournisseur.getPhoto());
                    Files.deleteIfExists(photoPath);
                } catch (IOException e) {
                }
            }
        }
        fournisseurRepository.deleteAll(fournisseurs);

        // 12.3. Supprimer tous les StockProduitFournisseur AVANT les Stock (car ils référencent stock_id)
        List<Boutique> boutiquesForStockProduitFournisseur = boutiqueRepository.findByEntrepriseId(entrepriseId);
        int totalStockProduitFournisseur = 0;
        for (Boutique boutique : boutiquesForStockProduitFournisseur) {
            List<Stock> stocks = stockRepository.findByBoutiqueId(boutique.getId());
            for (Stock stock : stocks) {
                // Utiliser une requête native pour supprimer directement les StockProduitFournisseur
                int deleted = entityManager.createNativeQuery(
                    "DELETE FROM stock_produit_fournisseur WHERE stock_id = :stockId"
                ).setParameter("stockId", stock.getId()).executeUpdate();
                totalStockProduitFournisseur += deleted;
            }
        }
        if (totalStockProduitFournisseur > 0) {
            entityManager.flush();
        }

        // 12.4. Supprimer tous les Stock AVANT les produits (car ils référencent produit_id)
        List<Boutique> boutiquesForStocks = boutiqueRepository.findByEntrepriseId(entrepriseId);
        int totalStocks = 0;
        for (Boutique boutique : boutiquesForStocks) {
            List<Stock> stocks = stockRepository.findByBoutiqueId(boutique.getId());
            if (!stocks.isEmpty()) {
                totalStocks += stocks.size();
                stockRepository.deleteAll(stocks);
            }
        }
        if (totalStocks > 0) {
            entityManager.flush();
        }

        // 12.4.5. Supprimer toutes les Interaction qui référencent des Produit AVANT les produits (car elles référencent produit_id)
        // Utiliser une requête native pour supprimer toutes les interactions liées aux produits de l'entreprise
        int totalInteractionsProduit = entityManager.createNativeQuery(
            "DELETE i FROM interactions i " +
            "INNER JOIN produit p ON i.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND i.produit_id IS NOT NULL"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalInteractionsProduit > 0) {
            entityManager.flush();
        }

        // 12.4.6. Supprimer tous les ProspectAchat qui référencent des Produit AVANT les produits (car ils référencent produit_id)
        // Utiliser une requête native pour supprimer tous les achats de prospection liés aux produits de l'entreprise
        int totalProspectAchatsProduit = entityManager.createNativeQuery(
            "DELETE pa FROM prospect_achat pa " +
            "INNER JOIN produit p ON pa.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND pa.produit_id IS NOT NULL"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalProspectAchatsProduit > 0) {
            entityManager.flush();
        }

        // 12.5. Supprimer les produits avec leurs photos AVANT les catégories (car ils référencent categorie_id)
        List<Produit> produits = produitRepository.findByEntrepriseId(entrepriseId);
        for (Produit produit : produits) {
            // Supprimer la photo du produit
            if (produit.getPhoto() != null && !produit.getPhoto().isBlank()) {
                try {
                    Path photoPath = Paths.get("src/main/resources/static" + produit.getPhoto());
                    Files.deleteIfExists(photoPath);
                } catch (IOException e) {
                }
            }
        }
        if (!produits.isEmpty()) {
            produitRepository.deleteAll(produits);
            entityManager.flush();
        }
        
        // 12.5.5. Vérification supplémentaire : supprimer tous les produits restants via requête native
        // pour s'assurer qu'aucun produit ne reste avant de supprimer les catégories
        Long countProduitsRestants = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM produit p " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        if (countProduitsRestants > 0) {
            // Supprimer tous les produits restants via requête native
            entityManager.createNativeQuery(
                "DELETE p FROM produit p " +
                "INNER JOIN boutique b ON p.boutique_id = b.id " +
                "WHERE b.entreprise_id = :entrepriseId"
            ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            entityManager.flush();
        }

        // 13. Supprimer toutes les catégories (maintenant que tous les produits sont supprimés)
        List<Categorie> categories = categorieRepository.findByEntrepriseId(entrepriseId);
        categorieRepository.deleteAll(categories);

        // 14. Supprimer toutes les unités
        List<Unite> unites = uniteRepository.findByEntrepriseId(entrepriseId);
        uniteRepository.deleteAll(unites);

        // 15. Supprimer toutes les catégories de dépenses
        List<CategorieDepense> categoriesDepenses = categorieDepenseRepository.findByEntrepriseId(entrepriseId);
        categorieDepenseRepository.deleteAll(categoriesDepenses);

        // 15.5. Supprimer toutes les interactions et achats de prospection AVANT les prospects (car ils référencent prospect qui référence entreprise)
        List<Prospect> prospectsForInteractions = prospectRepository.findByEntrepriseId(entrepriseId, Pageable.unpaged()).getContent();
        int totalInteractions = 0;
        int totalAchats = 0;
        for (Prospect prospect : prospectsForInteractions) {
            // Supprimer les interactions de ce prospect
            List<Interaction> interactions = interactionRepository.findByProspectIdOrderByOccurredAtDesc(prospect.getId());
            if (!interactions.isEmpty()) {
                totalInteractions += interactions.size();
                interactionRepository.deleteAll(interactions);
            }
            
            // Supprimer les achats de ce prospect
            List<ProspectAchat> achats = prospectAchatRepository.findByProspectId(prospect.getId());
            if (!achats.isEmpty()) {
                totalAchats += achats.size();
                prospectAchatRepository.deleteAll(achats);
            }
        }
        if (totalInteractions > 0) {
        }
        if (totalAchats > 0) {
        }
        if (totalInteractions > 0 || totalAchats > 0) {
            entityManager.flush();
        }

        // 16. Supprimer tous les prospects
        List<Prospect> prospects = prospectRepository.findByEntrepriseId(entrepriseId, Pageable.unpaged()).getContent();
        prospectRepository.deleteAll(prospects);
        entityManager.flush();

        // 17. Supprimer tous les paiements de modules
        List<PaiementModule> paiementsModules = paiementModuleRepository.findByEntrepriseId(entrepriseId);
        paiementModuleRepository.deleteAll(paiementsModules);

        // 18. Supprimer tous les essais de modules
        List<EntrepriseModuleEssai> essaisModules = entrepriseModuleEssaiRepository.findByEntreprise(entreprise);
        entrepriseModuleEssaiRepository.deleteAll(essaisModules);

        // 19. Supprimer tous les abonnements de modules
        List<EntrepriseModuleAbonnement> abonnementsModules = entrepriseModuleAbonnementRepository.findByEntrepriseAndActifTrue(entreprise); 
        entrepriseModuleAbonnementRepository.deleteAll(abonnementsModules);


        // 21. Supprimer les boutiques (les stocks ont déjà été supprimés dans la section 12.4)
        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        boutiqueRepository.deleteAll(boutiques);
        entityManager.flush();

        // 22. Supprimer les fichiers images de l'entreprise (logo, signature, cachet)
        if (entreprise.getLogo() != null && !entreprise.getLogo().isBlank()) {
            try {
                Path logoPath = Paths.get("src/main/resources/static" + entreprise.getLogo());
                Files.deleteIfExists(logoPath);
            } catch (IOException e) {
            }
        }

        if (entreprise.getSignaturNum() != null && !entreprise.getSignaturNum().isBlank()) {
            try {
                Path signaturePath = Paths.get("src/main/resources/static" + entreprise.getSignaturNum());
                Files.deleteIfExists(signaturePath);
            } catch (IOException e) {
            }
        }

        if (entreprise.getCachetNum() != null && !entreprise.getCachetNum().isBlank()) {
            try {
                Path cachetPath = Paths.get("src/main/resources/static" + entreprise.getCachetNum());
                Files.deleteIfExists(cachetPath);
            } catch (IOException e) {
            }
        }

        // 22.5. Vider la relation ManyToMany avec AppModule avant de supprimer l'entreprise
        entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        if (entreprise != null) {
            // Vider la collection modulesActifs pour éviter les problèmes avec la table de jointure
            if (entreprise.getModulesActifs() != null && !entreprise.getModulesActifs().isEmpty()) {
                entreprise.getModulesActifs().clear();
                entrepriseRepository.save(entreprise);
            }
        }

        // 22.6. Forcer un flush et clear la session Hibernate pour éviter les références persistantes
        entityManager.flush();
        entityManager.clear();

        // 23. Supprimer l'entreprise elle-même
        // Recharger l'entreprise depuis la base (session propre maintenant)
        entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        if (entreprise != null) {
            // S'assurer que toutes les collections sont vides
            if (entreprise.getUtilisateurs() != null) {
                entreprise.getUtilisateurs().clear();
            }
            if (entreprise.getBoutiques() != null) {
                entreprise.getBoutiques().clear();
            }
            if (entreprise.getFacturesProforma() != null) {
                entreprise.getFacturesProforma().clear();
            }
            if (entreprise.getModulesActifs() != null) {
                entreprise.getModulesActifs().clear();
            }
            entreprise.setAdmin(null);
            
            // Sauvegarder avant de supprimer pour s'assurer que toutes les relations sont nettoyées
            entrepriseRepository.save(entreprise);
            
            // Forcer un flush avant la suppression
            entityManager.flush();
            
            // Maintenant supprimer
            entrepriseRepository.delete(entreprise);
            
            // Forcer un flush final pour s'assurer que la suppression est effectuée
            entityManager.flush();
        } else {
        }

    }

    /**
     * Supprime toutes les données de l'entreprise mais garde l'admin et l'entreprise.
     * 
     * Cette méthode supprime toutes les données associées à l'entreprise :
     * - Tous les utilisateurs SAUF l'admin
     * - Toutes les données métier (ventes, produits, clients, factures, etc.)
     * - Tous les fichiers/images associés
     * 
     * L'admin et l'entreprise sont conservés pour permettre une réinitialisation complète.
     * 
     * Réservé à l'admin lui-même (utilise son propre ID depuis le token JWT).
     * 
     *  user L'admin qui effectue l'opération (doit être un ADMIN)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteEntrepriseDataButKeepAdmin(User user) {
        // Vérifier que l'utilisateur est bien un admin
        if (user.getRole() == null || user.getRole().getName() != RoleType.ADMIN) {
            throw new RuntimeException("Accès refusé : vous devez être administrateur pour vider votre entreprise.");
        }

        // Utiliser directement l'ID de l'utilisateur connecté
        Long adminId = user.getId();
        User admin = user;

        // 3. Récupérer l'entreprise de l'admin
        Entreprise entreprise = admin.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'admin n'a pas d'entreprise associée.");
        }

        Long entrepriseId = entreprise.getId();

        // 3.1. Identifier la première boutique à conserver (par ID le plus petit)
        List<Boutique> toutesBoutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        Boutique premiereBoutique = null;
        final Long finalPremiereBoutiqueId;
        if (!toutesBoutiques.isEmpty()) {
            premiereBoutique = toutesBoutiques.stream()
                    .min(Comparator.comparing(Boutique::getId))
                    .orElse(null);
            finalPremiereBoutiqueId = (premiereBoutique != null) ? premiereBoutique.getId() : null;
        } else {
            finalPremiereBoutiqueId = null;
        }

        // 3. Retirer la référence admin de l'entreprise temporairement (pour éviter les erreurs)
        entreprise.setAdmin(null);
        entrepriseRepository.save(entreprise);

        // 4. Supprimer toutes les dépenses générales AVANT les utilisateurs (car elles référencent creePar)
        List<DepenseGenerale> depenses = depenseGeneraleRepository.findByEntrepriseId(entrepriseId);
        for (DepenseGenerale depense : depenses) {
            if (depense.getPieceJointe() != null && !depense.getPieceJointe().isBlank()) {
                try {
                    Path pieceJointePath = Paths.get("src/main/resources/static" + depense.getPieceJointe());
                    Files.deleteIfExists(pieceJointePath);
                } catch (IOException e) {
                }
            }
        }
        depenseGeneraleRepository.deleteAll(depenses);

        // 5. Supprimer toutes les entrées générales AVANT les utilisateurs (car elles référencent creePar et responsable)
        List<EntreeGenerale> entrees = entreeGeneraleRepository.findByEntrepriseId(entrepriseId);
        for (EntreeGenerale entree : entrees) {
            if (entree.getPieceJointe() != null && !entree.getPieceJointe().isBlank()) {
                try {
                    Path pieceJointePath = Paths.get("src/main/resources/static" + entree.getPieceJointe());
                    Files.deleteIfExists(pieceJointePath);
                } catch (IOException e) {
                }
            }
        }
        entreeGeneraleRepository.deleteAll(entrees);

        // 5.5. Supprimer tous les transferts de fonds AVANT les utilisateurs (car ils référencent creePar)
        List<TransfertFonds> transferts = transfertFondsRepository.findByEntrepriseIdOrderByDateTransfertDesc(entrepriseId);
        transfertFondsRepository.deleteAll(transferts);

        // 5.6. Supprimer toutes les notifications globales AVANT les utilisateurs (car elles référencent recipient)
        List<User> usersForNotifications = usersRepository.findByEntrepriseId(entrepriseId);
        for (User u : usersForNotifications) {
            List<GlobalNotification> notifications = globalNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(u.getId());
            if (!notifications.isEmpty()) {
                globalNotificationRepository.deleteAll(notifications);
            }
        }

        // 5.6.5. Supprimer tous les FactureProduit AVANT les factures (car ils référencent facture qui référence user)
        List<Boutique> boutiquesForFactureProduit = boutiqueRepository.findByEntrepriseId(entrepriseId);
        int totalFactureProduits = 0;
        for (Boutique boutique : boutiquesForFactureProduit) {
            List<Facture> facturesBoutique = factureRepository.findByBoutiqueIdAndEntrepriseId(
                    boutique.getId(), entrepriseId);
            for (Facture facture : facturesBoutique) {
                int deleted = entityManager.createNativeQuery(
                    "DELETE FROM facture_produit WHERE facture_id = :factureId"
                ).setParameter("factureId", facture.getId()).executeUpdate();
                totalFactureProduits += deleted;
            }
        }
        if (totalFactureProduits > 0) {
            entityManager.flush();
        }

        // 5.7. Supprimer toutes les factures (liées aux boutiques) AVANT les utilisateurs (car elles référencent user)
        List<Facture> factures = factureRepository.findAllByEntrepriseId(entrepriseId);
        factureRepository.deleteAll(factures);
        entityManager.flush();

        // 5.7.5. Supprimer tous les mouvements de caisse AVANT les versements comptables (car ils référencent caisse)
        List<Boutique> boutiquesForMouvements = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForMouvements) {
            List<Caisse> caissesBoutique = caisseRepository.findByBoutiqueId(boutique.getId());
            for (Caisse caisse : caissesBoutique) {
                List<MouvementCaisse> allMouvements = mouvementCaisseRepository.findByCaisseId(caisse.getId());
                if (!allMouvements.isEmpty()) {
                    mouvementCaisseRepository.deleteAll(allMouvements);
                }
            }
        }

        // 5.8. Supprimer tous les versements comptables AVANT les utilisateurs (car ils référencent creePar)
        List<Boutique> boutiquesForVersements = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForVersements) {
            List<VersementComptable> versements = versementComptableRepository.findByCaisse_BoutiqueId(boutique.getId());
            if (!versements.isEmpty()) {
                versementComptableRepository.deleteAll(versements);
            }
        }

        // 5.9. Supprimer toutes les relations UserBoutique AVANT les utilisateurs
        for (User u : usersForNotifications) {
            List<UserBoutique> userBoutiques = userBoutiqueRepository.findByUserId(u.getId());
            if (!userBoutiques.isEmpty()) {
                userBoutiqueRepository.deleteAll(userBoutiques);
            }
        }

        // 5.9.5. Supprimer tous les PasswordResetToken AVANT les utilisateurs (car ils référencent User)
        for (User u : usersForNotifications) {
            passwordResetTokenRepository.deleteByUser(u);
        }
        entityManager.flush();

        // 5.9.5.5. Supprimer tous les InitialPasswordToken AVANT les utilisateurs (car ils référencent User)
        for (User u : usersForNotifications) {
            initialPasswordTokenRepository.deleteByUser(u);
        }
        entityManager.flush();

        // 5.9.6. Supprimer tous les paiements AVANT les utilisateurs (car ils référencent User encaissePar)
        List<FactureReelle> facturesReellesForPaiements = factureReelleRepository.findByEntrepriseId(entrepriseId);
        for (FactureReelle factureReelle : facturesReellesForPaiements) {
            int deleted = entityManager.createNativeQuery(
                "DELETE FROM paiement WHERE facture_reelle_id = :factureReelleId"
            ).setParameter("factureReelleId", factureReelle.getId()).executeUpdate();
        }
        entityManager.flush();

        // 5.9.7. Mettre à null toutes les références User dans les FactureReelle AVANT de supprimer les User
        int facturesReellesUpdated = 0;
        for (FactureReelle factureReelle : facturesReellesForPaiements) {
            boolean updated = false;
            if (factureReelle.getUtilisateurCreateur() != null) {
                factureReelle.setUtilisateurCreateur(null);
                updated = true;
            }
            if (factureReelle.getUtilisateurAnnulateur() != null) {
                factureReelle.setUtilisateurAnnulateur(null);
                updated = true;
            }
            if (factureReelle.getUtilisateurValidateur() != null) {
                factureReelle.setUtilisateurValidateur(null);
                updated = true;
            }
            if (updated) {
                factureReelleRepository.save(factureReelle);
                facturesReellesUpdated++;
            }
        }
        if (facturesReellesUpdated > 0) {
            entityManager.flush();
        }

        // 5.9.8. Mettre à null toutes les références User dans les FactureProForma AVANT de supprimer les User
        List<FactureProForma> facturesProformaForNull = factureProformaRepository.findByEntrepriseId(entrepriseId);
        int facturesProformaUpdated = 0;
        for (FactureProForma factureProforma : facturesProformaForNull) {
            boolean updated = false;
            if (factureProforma.getUtilisateurCreateur() != null) {
                factureProforma.setUtilisateurCreateur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurValidateur() != null) {
                factureProforma.setUtilisateurValidateur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurModificateur() != null) {
                factureProforma.setUtilisateurModificateur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurRelanceur() != null) {
                factureProforma.setUtilisateurRelanceur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurApprobateur() != null) {
                factureProforma.setUtilisateurApprobateur(null);
                updated = true;
            }
            if (factureProforma.getUtilisateurAnnulateur() != null) {
                factureProforma.setUtilisateurAnnulateur(null);
                updated = true;
            }
            if (factureProforma.getApprobateurs() != null && !factureProforma.getApprobateurs().isEmpty()) {
                factureProforma.getApprobateurs().clear();
                updated = true;
            }
            if (updated) {
                factureProformaRepository.save(factureProforma);
                facturesProformaUpdated++;
            }
        }
        if (facturesProformaUpdated > 0) {
            entityManager.flush();
        }

        // 5.9.9. Supprimer toutes les NoteFactureProForma AVANT les utilisateurs (car elles référencent User auteur)
        List<FactureProForma> facturesProformaForNotes = factureProformaRepository.findByEntrepriseId(entrepriseId);
        for (FactureProForma facture : facturesProformaForNotes) {
            // Supprimer les notes de cette facture (isolé par entreprise)
            Long factureEntrepriseId = facture.getEntreprise() != null ? facture.getEntreprise().getId() : null;
            if (factureEntrepriseId != null) {
                List<NoteFactureProForma> notes = noteFactureProFormaRepository.findByFactureProFormaIdAndEntrepriseId(
                        facture.getId(), factureEntrepriseId);
                if (!notes.isEmpty()) {
                    noteFactureProFormaRepository.deleteAll(notes);
                }
            }
        }
        entityManager.flush();

        // 5.9.10. Supprimer tous les FactProHistoriqueAction AVANT les utilisateurs (car ils référencent User utilisateur)
        List<FactureProForma> facturesProformaForHistoriques = factureProformaRepository.findByEntrepriseId(entrepriseId);
        for (FactureProForma facture : facturesProformaForHistoriques) {
            factProHistoriqueActionRepository.deleteByFacture(facture);
        }
        entityManager.flush();

        // 5.9.11. Supprimer tous les StockHistory AVANT les utilisateurs (car ils référencent User)
        int totalStockHistories = entityManager.createNativeQuery(
            "DELETE sh FROM stock_history sh " +
            "INNER JOIN stock s ON sh.stock_id = s.id " +
            "INNER JOIN produit p ON s.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalStockHistories > 0) {
            entityManager.flush();
        }

        // 5.10. Supprimer d'abord les entités qui référencent les ventes, puis les ventes elles-mêmes
        List<Vente> ventesWithReferences = venteRepository.findAllByEntrepriseId(entrepriseId);
        
        // ÉTAPE 1 : Supprimer les historiques et factures de vente AVANT de modifier les ventes
        int totalHistoriques = 0;
        int[] totalFacturesVente = {0};
        for (Vente vente : ventesWithReferences) {
            List<VenteHistorique> historiques = venteHistoriqueRepository.findByVenteId(vente.getId());
            if (!historiques.isEmpty()) {
                totalHistoriques += historiques.size();
                venteHistoriqueRepository.deleteAll(historiques);
            }
            
            // Supprimer les factures de vente pour cette vente spécifique (isolé par entreprise)
            Long venteEntrepriseId = vente.getBoutique() != null && vente.getBoutique().getEntreprise() != null 
                    ? vente.getBoutique().getEntreprise().getId() : null;
            if (venteEntrepriseId != null) {
                factureVenteRepository.findByVenteIdAndEntrepriseId(vente.getId(), venteEntrepriseId)
                        .ifPresent(factureVente -> {
                            factureVenteRepository.delete(factureVente);
                            totalFacturesVente[0]++;
                        });
            }
        }
        
        List<FactureVente> facturesVenteRestantes = factureVenteRepository.findAllByEntrepriseId(entrepriseId);
        if (!facturesVenteRestantes.isEmpty()) {
            factureVenteRepository.deleteAll(facturesVenteRestantes);
        }
        
        entityManager.flush();
        
        // ÉTAPE 2 : Maintenant mettre à null les références dans les ventes
        int ventesUpdated = 0;
        for (Vente vente : ventesWithReferences) {
            boolean updated = false;
            if (vente.getCaisse() != null) {
                vente.setCaisse(null);
                updated = true;
            }
            if (vente.getVendeur() != null) {
                vente.setVendeur(null);
                updated = true;
            }
            if (vente.getBoutique() != null) {
                vente.setBoutique(null);
                updated = true;
            }
            if (vente.getClient() != null) {
                vente.setClient(null);
                updated = true;
            }
            if (vente.getEntrepriseClient() != null) {
                vente.setEntrepriseClient(null);
                updated = true;
            }
            if (updated) {
                venteRepository.save(vente);
                ventesUpdated++;
            }
        }
        if (ventesUpdated > 0) {
            entityManager.flush();
        }
        
        // ÉTAPE 3 : Maintenant supprimer les ventes elles-mêmes
        if (!ventesWithReferences.isEmpty()) {
            venteRepository.deleteAll(ventesWithReferences);
            entityManager.flush();
        }

        // 5.11. Supprimer toutes les caisses AVANT les utilisateurs (car elles référencent vendeur_id)
        List<Boutique> boutiquesForCaisses = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForCaisses) {
            List<Caisse> caisses = caisseRepository.findByBoutiqueId(boutique.getId());
            if (!caisses.isEmpty()) {
                caisseRepository.deleteAll(caisses);
            }
        }

        // 5.12. Supprimer toutes les sessions des utilisateurs de cette entreprise SAUF celle de l'admin
        // Supprimer les sessions de tous les utilisateurs sauf l'admin
        entityManager.createNativeQuery(
                "DELETE us FROM user_sessions us " +
                "INNER JOIN user u ON us.user_id = u.id " +
                "WHERE u.entreprise_id = :entrepriseId AND u.id != :adminId"
        ).setParameter("entrepriseId", entrepriseId)
         .setParameter("adminId", adminId)
         .executeUpdate();
        entityManager.flush();

        // 6. Supprimer tous les utilisateurs de l'entreprise SAUF l'admin (avec leurs fichiers)
        List<User> users = usersRepository.findByEntrepriseId(entrepriseId);
        for (User utilisateur : users) {
            // Ne pas supprimer l'admin
            if (utilisateur.getId().equals(adminId)) {
                continue;
            }
            
            // Supprimer le QR Code de l'utilisateur
            if (utilisateur.getQrCodeUrl() != null && !utilisateur.getQrCodeUrl().isBlank()) {
                try {
                    imageStorageService.deleteQrCodeImage(utilisateur.getQrCodeUrl());
                } catch (Exception e) {
                }
            }
            // Supprimer la photo de l'utilisateur
            if (utilisateur.getPhoto() != null && !utilisateur.getPhoto().isBlank()) {
                try {
                    Path photoPath = Paths.get("src/main/resources/static" + utilisateur.getPhoto());
                    Files.deleteIfExists(photoPath);
                } catch (IOException e) {
                }
            }
        }
        // Supprimer tous les utilisateurs sauf l'admin
        users.removeIf(u -> u.getId().equals(adminId));
        if (!users.isEmpty()) {
            usersRepository.deleteAll(users);
            entityManager.flush();
        }

        // 7. Supprimer toutes les factures réelles
        List<FactureReelle> facturesReelles = factureReelleRepository.findByEntrepriseId(entrepriseId);
        factureReelleRepository.deleteAll(facturesReelles);
        entityManager.flush();

        // 9. Supprimer toutes les factures proforma
        List<FactureProForma> facturesProforma = factureProformaRepository.findByEntrepriseId(entrepriseId);
        factureProformaRepository.deleteAll(facturesProforma);
        entityManager.flush();

        // 10. Supprimer tous les clients (directement liés et via EntrepriseClient) avec leurs photos
        List<Client> clients = clientRepository.findClientsByEntrepriseOrEntrepriseClient(entrepriseId);
        for (Client client : clients) {
            if (client.getPhoto() != null && !client.getPhoto().isBlank()) {
                try {
                    Path photoPath = Paths.get("src/main/resources/static" + client.getPhoto());
                    Files.deleteIfExists(photoPath);
                } catch (IOException e) {
                }
            }
        }
        if (!clients.isEmpty()) {
            clientRepository.deleteAll(clients);
            entityManager.flush();
        }
        
        // Vérification supplémentaire avec une requête native pour s'assurer qu'il ne reste aucun client
        Long countClientsRestants = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM client WHERE entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        if (countClientsRestants > 0) {
            entityManager.createNativeQuery(
                "DELETE FROM client WHERE entreprise_id = :entrepriseId"
            ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            entityManager.flush();
        }

        // 11. Supprimer toutes les entreprises clients
        List<EntrepriseClient> entreprisesClients = entrepriseClientRepository.findByEntrepriseId(entrepriseId);
        if (!entreprisesClients.isEmpty()) {
            entrepriseClientRepository.deleteAll(entreprisesClients);
            entityManager.flush();
        }
        
        // Vérification finale : s'assurer qu'il ne reste aucun client ou entreprise client
        Long countClientsFinal = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM client WHERE entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        Long countEntreprisesClientsFinal = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM entreprise_client WHERE entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        if (countClientsFinal > 0 || countEntreprisesClientsFinal > 0) {
            if (countClientsFinal > 0) {
                entityManager.createNativeQuery(
                    "DELETE FROM client WHERE entreprise_id = :entrepriseId"
                ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            }
            if (countEntreprisesClientsFinal > 0) {
                entityManager.createNativeQuery(
                    "DELETE FROM entreprise_client WHERE entreprise_id = :entrepriseId"
                ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            }
            entityManager.flush();
        }

        // 12. Supprimer tous les fournisseurs avec leurs photos (isolé par entreprise)
        List<Fournisseur> fournisseurs = fournisseurRepository.findByEntrepriseId(entrepriseId);
        for (Fournisseur fournisseur : fournisseurs) {
            if (fournisseur.getPhoto() != null && !fournisseur.getPhoto().isBlank()) {
                try {
                    Path photoPath = Paths.get("src/main/resources/static" + fournisseur.getPhoto());
                    Files.deleteIfExists(photoPath);
                } catch (IOException e) {
                }
            }
        }
        fournisseurRepository.deleteAll(fournisseurs);

        // 12.3. Supprimer tous les StockProduitFournisseur AVANT les Stock (car ils référencent stock_id)
        List<Boutique> boutiquesForStockProduitFournisseur = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForStockProduitFournisseur) {
            List<Stock> stocks = stockRepository.findByBoutiqueId(boutique.getId());
            for (Stock stock : stocks) {
                int deleted = entityManager.createNativeQuery(
                    "DELETE FROM stock_produit_fournisseur WHERE stock_id = :stockId"
                ).setParameter("stockId", stock.getId()).executeUpdate();
            }
        }
        entityManager.flush();

        // 12.4. Supprimer tous les Stock AVANT les produits (car ils référencent produit_id)
        List<Boutique> boutiquesForStocks = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForStocks) {
            List<Stock> stocks = stockRepository.findByBoutiqueId(boutique.getId());
            if (!stocks.isEmpty()) {
                stockRepository.deleteAll(stocks);
            }
        }
        entityManager.flush();

        // 12.4.5. Supprimer toutes les Interaction qui référencent des Produit AVANT les produits (car elles référencent produit_id)
        int totalInteractionsProduit = entityManager.createNativeQuery(
            "DELETE i FROM interactions i " +
            "INNER JOIN produit p ON i.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND i.produit_id IS NOT NULL"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalInteractionsProduit > 0) {
            entityManager.flush();
        }

        // 12.4.6. Supprimer tous les ProspectAchat qui référencent des Produit AVANT les produits (car ils référencent produit_id)
        int totalProspectAchatsProduit = entityManager.createNativeQuery(
            "DELETE pa FROM prospect_achat pa " +
            "INNER JOIN produit p ON pa.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND pa.produit_id IS NOT NULL"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalProspectAchatsProduit > 0) {
            entityManager.flush();
        }

        // 12.5. Supprimer les produits avec leurs photos AVANT les catégories (car ils référencent categorie_id)
        // Exclure les produits de la première boutique (ils seront supprimés dans la section 12.5.5)
        List<Produit> produits = produitRepository.findByEntrepriseId(entrepriseId);
        if (finalPremiereBoutiqueId != null) {
            produits = produits.stream()
                    .filter(p -> p.getBoutique() == null || !p.getBoutique().getId().equals(finalPremiereBoutiqueId))
                    .collect(Collectors.toList());
        }
        for (Produit produit : produits) {
            if (produit.getPhoto() != null && !produit.getPhoto().isBlank()) {
                try {
                    Path photoPath = Paths.get("src/main/resources/static" + produit.getPhoto());
                    Files.deleteIfExists(photoPath);
                } catch (IOException e) {
                }
            }
        }
        if (!produits.isEmpty()) {
            produitRepository.deleteAll(produits);
            entityManager.flush();
        }

        // 12.5.5. Supprimer les produits de la première boutique AVANT les catégories
        if (premiereBoutique != null) {
            List<Produit> produitsPremiereBoutique = produitRepository.findByBoutique(premiereBoutique);
            for (Produit produit : produitsPremiereBoutique) {
                if (produit.getPhoto() != null && !produit.getPhoto().isBlank()) {
                    try {
                        Path photoPath = Paths.get("src/main/resources/static" + produit.getPhoto());
                        Files.deleteIfExists(photoPath);
                    } catch (IOException e) {
                    }
                }
            }
            if (!produitsPremiereBoutique.isEmpty()) {
                produitRepository.deleteAll(produitsPremiereBoutique);
                entityManager.flush();
            }
        }

        // 13. Supprimer toutes les catégories (maintenant que tous les produits sont supprimés)
        List<Categorie> categories = categorieRepository.findByEntrepriseId(entrepriseId);
        categorieRepository.deleteAll(categories);

        // 14. Supprimer toutes les unités
        List<Unite> unites = uniteRepository.findByEntrepriseId(entrepriseId);
        uniteRepository.deleteAll(unites);

        // 15. Supprimer toutes les catégories de dépenses
        List<CategorieDepense> categoriesDepenses = categorieDepenseRepository.findByEntrepriseId(entrepriseId);
        categorieDepenseRepository.deleteAll(categoriesDepenses);

        // 15.5. Supprimer toutes les interactions et achats de prospection AVANT les prospects (car ils référencent prospect qui référence entreprise)
        List<Prospect> prospectsForInteractions = prospectRepository.findByEntrepriseId(entrepriseId, Pageable.unpaged()).getContent();
        for (Prospect prospect : prospectsForInteractions) {
            List<Interaction> interactions = interactionRepository.findByProspectIdOrderByOccurredAtDesc(prospect.getId());
            if (!interactions.isEmpty()) {
                interactionRepository.deleteAll(interactions);
            }
            
            List<ProspectAchat> achats = prospectAchatRepository.findByProspectId(prospect.getId());
            if (!achats.isEmpty()) {
                prospectAchatRepository.deleteAll(achats);
            }
        }
        entityManager.flush();

        // 16. Supprimer tous les prospects
        List<Prospect> prospects = prospectRepository.findByEntrepriseId(entrepriseId, Pageable.unpaged()).getContent();
        prospectRepository.deleteAll(prospects);
        entityManager.flush();

        // 17. Supprimer tous les paiements de modules
        // List<PaiementModule> paiementsModules = paiementModuleRepository.findByEntrepriseId(entrepriseId);
        // paiementModuleRepository.deleteAll(paiementsModules);

        // 18. Supprimer tous les essais de modules
        // List<EntrepriseModuleEssai> essaisModules = entrepriseModuleEssaiRepository.findByEntreprise(entreprise);
        // entrepriseModuleEssaiRepository.deleteAll(essaisModules);

        // 19. Supprimer tous les abonnements de modules
        // List<EntrepriseModuleAbonnement> abonnementsModules = entrepriseModuleAbonnementRepository.findByEntrepriseAndActifTrue(entreprise); 
        // entrepriseModuleAbonnementRepository.deleteAll(abonnementsModules);

        // 21. Vider la première boutique et supprimer les autres
        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        if (premiereBoutique != null && !boutiques.isEmpty() && finalPremiereBoutiqueId != null) {
            Long premiereBoutiqueId = finalPremiereBoutiqueId;
            
            // 21.1. Vider le contenu de la première boutique
            
            // 21.1.1. Supprimer tous les FactureProduit de la première boutique
            List<Facture> facturesPremiereBoutique = factureRepository.findByBoutiqueIdAndEntrepriseId(
                    premiereBoutiqueId, entrepriseId);
            for (Facture facture : facturesPremiereBoutique) {
                entityManager.createNativeQuery(
                    "DELETE FROM facture_produit WHERE facture_id = :factureId"
                ).setParameter("factureId", facture.getId()).executeUpdate();
            }
            entityManager.flush();
            
            // 21.1.2. Supprimer toutes les factures de la première boutique
            factureRepository.deleteAll(facturesPremiereBoutique);
            entityManager.flush();
            
            // 21.1.3. Supprimer tous les mouvements de caisse de la première boutique
            List<Caisse> caissesPremiereBoutique = caisseRepository.findByBoutiqueId(premiereBoutiqueId);
            for (Caisse caisse : caissesPremiereBoutique) {
                List<MouvementCaisse> mouvements = mouvementCaisseRepository.findByCaisseId(caisse.getId());
                if (!mouvements.isEmpty()) {
                    mouvementCaisseRepository.deleteAll(mouvements);
                }
            }
            entityManager.flush();
            
            // 21.1.4. Supprimer tous les versements comptables de la première boutique
            List<VersementComptable> versementsPremiereBoutique = versementComptableRepository.findByCaisse_BoutiqueId(premiereBoutiqueId);
            if (!versementsPremiereBoutique.isEmpty()) {
                versementComptableRepository.deleteAll(versementsPremiereBoutique);
            }
            entityManager.flush();
            
            // 21.1.5. Supprimer toutes les caisses de la première boutique
            if (!caissesPremiereBoutique.isEmpty()) {
                caisseRepository.deleteAll(caissesPremiereBoutique);
            }
            entityManager.flush();
            
            // 21.1.6. Supprimer toutes les relations UserBoutique de la première boutique
            List<UserBoutique> userBoutiquesPremiere = userBoutiqueRepository.findByBoutiqueId(premiereBoutiqueId);
            if (!userBoutiquesPremiere.isEmpty()) {
                userBoutiqueRepository.deleteAll(userBoutiquesPremiere);
            }
            entityManager.flush();
            
            // 21.1.7. Supprimer tous les StockProduitFournisseur de la première boutique
            List<Stock> stocksPremiereBoutique = stockRepository.findByBoutiqueId(premiereBoutiqueId);
            for (Stock stock : stocksPremiereBoutique) {
                entityManager.createNativeQuery(
                    "DELETE FROM stock_produit_fournisseur WHERE stock_id = :stockId"
                ).setParameter("stockId", stock.getId()).executeUpdate();
            }
            entityManager.flush();
            
            // 21.1.8. Supprimer tous les Stock de la première boutique
            if (!stocksPremiereBoutique.isEmpty()) {
                stockRepository.deleteAll(stocksPremiereBoutique);
            }
            entityManager.flush();
            
            // 21.1.9. Supprimer toutes les Interactions liées aux produits de la première boutique
            int interactionsPremiereBoutique = entityManager.createNativeQuery(
                "DELETE i FROM interactions i " +
                "INNER JOIN produit p ON i.produit_id = p.id " +
                "WHERE p.boutique_id = :boutiqueId AND i.produit_id IS NOT NULL"
            ).setParameter("boutiqueId", premiereBoutiqueId).executeUpdate();
            if (interactionsPremiereBoutique > 0) {
                entityManager.flush();
            }
            
            // 21.1.10. Supprimer tous les ProspectAchat liés aux produits de la première boutique
            int prospectAchatsPremiereBoutique = entityManager.createNativeQuery(
                "DELETE pa FROM prospect_achat pa " +
                "INNER JOIN produit p ON pa.produit_id = p.id " +
                "WHERE p.boutique_id = :boutiqueId AND pa.produit_id IS NOT NULL"
            ).setParameter("boutiqueId", premiereBoutiqueId).executeUpdate();
            if (prospectAchatsPremiereBoutique > 0) {
                entityManager.flush();
            }
            
            // 21.1.11. Les produits de la première boutique ont déjà été supprimés dans la section 12.5.5
            // Pas besoin de les supprimer à nouveau ici
            
            // 21.1.12. Supprimer toutes les ventes de la première boutique
            List<Vente> ventesPremiereBoutique = venteRepository.findByBoutiqueId(premiereBoutiqueId);
            for (Vente vente : ventesPremiereBoutique) {
                // Supprimer les historiques de vente
                List<VenteHistorique> historiques = venteHistoriqueRepository.findByVenteId(vente.getId());
                if (!historiques.isEmpty()) {
                    venteHistoriqueRepository.deleteAll(historiques);
                }
                
                // Supprimer les factures de vente (isolé par entreprise)
                Long venteEntrepriseId = vente.getBoutique() != null && vente.getBoutique().getEntreprise() != null 
                        ? vente.getBoutique().getEntreprise().getId() : null;
                if (venteEntrepriseId != null) {
                    factureVenteRepository.findByVenteIdAndEntrepriseId(vente.getId(), venteEntrepriseId)
                            .ifPresent(factureVenteRepository::delete);
                }
            }
            entityManager.flush();
            
            // Mettre à null les références dans les ventes avant de les supprimer
            for (Vente vente : ventesPremiereBoutique) {
                vente.setCaisse(null);
                vente.setVendeur(null);
                vente.setBoutique(null);
                vente.setClient(null);
                vente.setEntrepriseClient(null);
                venteRepository.save(vente);
            }
            entityManager.flush();
            
            if (!ventesPremiereBoutique.isEmpty()) {
                venteRepository.deleteAll(ventesPremiereBoutique);
            }
            entityManager.flush();
            
            // 21.2. Supprimer toutes les autres boutiques (sauf la première)
            List<Boutique> autresBoutiques = boutiques.stream()
                    .filter(b -> !b.getId().equals(premiereBoutiqueId))
                    .collect(Collectors.toList());
            
            if (!autresBoutiques.isEmpty()) {
                boutiqueRepository.deleteAll(autresBoutiques);
            }
            entityManager.flush();
        } else {
            // Si pas de première boutique, supprimer toutes les boutiques
            if (!boutiques.isEmpty()) {
                boutiqueRepository.deleteAll(boutiques);
                entityManager.flush();
            }
        }

        // 22. Supprimer les fichiers images de l'entreprise (logo, signature, cachet)
        if (entreprise.getLogo() != null && !entreprise.getLogo().isBlank()) {
            try {
                Path logoPath = Paths.get("src/main/resources/static" + entreprise.getLogo());
                Files.deleteIfExists(logoPath);
            } catch (IOException e) {
            }
        }

        if (entreprise.getSignaturNum() != null && !entreprise.getSignaturNum().isBlank()) {
            try {
                Path signaturePath = Paths.get("src/main/resources/static" + entreprise.getSignaturNum());
                Files.deleteIfExists(signaturePath);
            } catch (IOException e) {
            }
        }

        if (entreprise.getCachetNum() != null && !entreprise.getCachetNum().isBlank()) {
            try {
                Path cachetPath = Paths.get("src/main/resources/static" + entreprise.getCachetNum());
                Files.deleteIfExists(cachetPath);
            } catch (IOException e) {
            }
        }

        // 22.5. CONSERVER les modules actifs, les essais de modules et les abonnements
        // On ne vide PAS les modules pour préserver les essais et abonnements
        // entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        // if (entreprise != null) {
        //     if (entreprise.getModulesActifs() != null && !entreprise.getModulesActifs().isEmpty()) {
        //         entreprise.getModulesActifs().clear();
        //         entrepriseRepository.save(entreprise);
        //     }
        // }

        // 22.6. Forcer un flush et clear la session Hibernate
        entityManager.flush();
        entityManager.clear();

        // 23. Remettre la référence admin sur l'entreprise (car on l'avait retirée au début)
        entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        if (entreprise != null) {
            // S'assurer que toutes les collections sont vides (SAUF les modules actifs pour conserver les essais)
            if (entreprise.getUtilisateurs() != null) {
                entreprise.getUtilisateurs().clear();
            }
            if (entreprise.getBoutiques() != null) {
                entreprise.getBoutiques().clear();
            }
            if (entreprise.getFacturesProforma() != null) {
                entreprise.getFacturesProforma().clear();
            }
            // CONSERVER les modules actifs pour préserver les essais et abonnements
            // if (entreprise.getModulesActifs() != null) {
            //     entreprise.getModulesActifs().clear();
            // }
            
            // Remettre l'admin sur l'entreprise
            entreprise.setAdmin(admin);
            
            // Sauvegarder l'entreprise avec l'admin
            entrepriseRepository.save(entreprise);
            entityManager.flush();
        }
    }

    /**
     * Déconnecte tous les utilisateurs du système (réservé SUPER_ADMIN).
     * 
     * Cette méthode :
     * 1. Supprime toutes les sessions actives de la base de données (si elles existent)
     * 2. Met à jour le lastActivity de tous les utilisateurs pour invalider leurs tokens JWT existants
     * 
     * Utile lors des mises à jour système pour forcer tous les utilisateurs à se reconnecter.
     * Fonctionne même si les utilisateurs n'ont pas de session (anciens tokens sans session).
     * 
     * @param superAdmin L'utilisateur SUPER_ADMIN qui effectue l'opération
     */
    @Transactional(rollbackFor = Exception.class)
    public void deconnecterTousLesUtilisateurs(User superAdmin) {
        ensureSuperAdmin(superAdmin);

        // 1. Supprimer toutes les sessions actives de la base de données (si elles existent)
        // Cela déconnecte les utilisateurs qui ont des sessions dans le nouveau système
        List<com.xpertcash.entity.UserSession> allSessions = userSessionRepository.findAll();
        if (!allSessions.isEmpty()) {
            userSessionRepository.deleteAll(allSessions);
            entityManager.flush();
        }

        // 2. Mettre à jour le lastActivity de tous les utilisateurs pour invalider leurs tokens JWT
        // Cela force TOUS les tokens existants (avec ou sans session) à être considérés comme révoqués
        // Lors de la prochaine requête, le système vérifie si userLastActivity > tokenLastActivity
        // et rejette le token si c'est le cas
        List<User> allUsers = usersRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (User user : allUsers) {
            user.setLastActivity(now);
            usersRepository.save(user);
        }
        entityManager.flush();

        // Résultat : Tous les utilisateurs devront se reconnecter car :
        // - Leurs sessions sont supprimées (s'ils en avaient)
        // - Leurs tokens JWT sont invalidés (userLastActivity > tokenLastActivity)
    }
}



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

   
    public void ensureSuperAdmin(User user) {
        if (user == null || user.getRole() == null || user.getRole().getName() != RoleType.SUPER_ADMIN) {
            throw new RuntimeException("Accès refusé : réservé au SUPER_ADMIN.");
        }
    }

    
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

  
    public void desactiverEntreprise(User superAdmin, Long entrepriseId) {
        ensureSuperAdmin(superAdmin);
        Entreprise entreprise = entrepriseService.getEntrepriseById(entrepriseId);
        entreprise.setActive(false);
        entrepriseRepository.save(entreprise);
    }

 
    public void activerEntreprise(User superAdmin, Long entrepriseId) {
        ensureSuperAdmin(superAdmin);
        Entreprise entreprise = entrepriseService.getEntrepriseById(entrepriseId);
        entreprise.setActive(true);
        entrepriseRepository.save(entreprise);
    }

 
    public SuperAdminEntrepriseStatsDTO getEntrepriseStats(User superAdmin, Long entrepriseId) {
        ensureSuperAdmin(superAdmin);

        Entreprise entreprise = entrepriseService.getEntrepriseById(entrepriseId);

        Long id = entreprise.getId();

        long nombreUtilisateurs = usersRepository.countByEntrepriseId(id);

        long nombreBoutiques = boutiqueRepository.countByEntrepriseId(id);

        long nombreProduits = produitRepository.countProduitsUniquesByEntrepriseId(id);

        long nombreStocks = stockRepository.countByEntrepriseId(id);

        long particuliers = clientRepository.countClientsDirectByEntrepriseId(id);
        long entreprisesClients = clientRepository.countClientsEntrepriseByEntrepriseId(id);
        long totalClients = particuliers + entreprisesClients;

        long nombreProspects = prospectRepository.countByEntrepriseId(id);

        long nombreFacturesProforma = factureProformaRepository.countFacturesByEntrepriseId(id);
        long nombreFacturesReelles = factureReelleRepository.countByEntrepriseId(id);

        long nombreCaissesOuvertes = caisseRepository.countByEntrepriseIdAndStatut(id, StatutCaisse.OUVERTE);

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

    // Supprime un Admin et TOUTES les données associées à son entreprise.

    @Transactional(rollbackFor = Exception.class)
    public void deleteAdminAndEntreprise(User user, Long adminId) {
        boolean isSuperAdmin = user.getRole() != null && user.getRole().getName() == RoleType.SUPER_ADMIN;
        boolean isSelf = user.getId().equals(adminId);
        
        if (!isSuperAdmin && !isSelf) {
            throw new RuntimeException("Accès refusé : vous ne pouvez supprimer que votre propre compte ou être SUPER_ADMIN.");
        }

        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin introuvable avec l'ID : " + adminId));

        if (admin.getRole() == null || admin.getRole().getName() != RoleType.ADMIN) {
            throw new RuntimeException("L'utilisateur avec l'ID " + adminId + " n'est pas un administrateur.");
        }

        if (!isSuperAdmin && isSelf) {
            if (user.getRole() == null || user.getRole().getName() != RoleType.ADMIN) {
                throw new RuntimeException("Vous devez être administrateur pour supprimer votre compte.");
            }
            if (user.getEntreprise() == null || admin.getEntreprise() == null ||
                !user.getEntreprise().getId().equals(admin.getEntreprise().getId())) {
                throw new RuntimeException("Vous ne pouvez supprimer que votre propre compte.");
            }
        }

        Entreprise entreprise = admin.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'admin n'a pas d'entreprise associée.");
        }

        Long entrepriseId = entreprise.getId();

     
        entreprise.setAdmin(null);
        entrepriseRepository.save(entreprise);

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

        List<TransfertFonds> transferts = transfertFondsRepository.findByEntrepriseIdOrderByDateTransfertDesc(entrepriseId);
        transfertFondsRepository.deleteAll(transferts);

        List<User> usersForNotifications = usersRepository.findByEntrepriseId(entrepriseId);
        for (User u : usersForNotifications) {
            List<GlobalNotification> notifications = globalNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(u.getId());
            if (!notifications.isEmpty()) {
                globalNotificationRepository.deleteAll(notifications);
            }
        }

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

        List<Facture> factures = factureRepository.findAllByEntrepriseId(entrepriseId);
        factureRepository.deleteAll(factures);
        entityManager.flush();

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

        List<Boutique> boutiquesForVersements = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForVersements) {
            List<VersementComptable> versements = versementComptableRepository.findByCaisse_BoutiqueId(boutique.getId());
            if (!versements.isEmpty()) {
                versementComptableRepository.deleteAll(versements);
            }
        }

        for (User u : usersForNotifications) {
            List<UserBoutique> userBoutiques = userBoutiqueRepository.findByUserId(u.getId());
            if (!userBoutiques.isEmpty()) {
                userBoutiqueRepository.deleteAll(userBoutiques);
            }
        }

        int totalPasswordTokens = 0;
        for (User u : usersForNotifications) {
            passwordResetTokenRepository.deleteByUser(u);
            totalPasswordTokens++;
        }
        if (totalPasswordTokens > 0) {
            entityManager.flush();
        }

        int totalInitialPasswordTokens = 0;
        for (User u : usersForNotifications) {
            initialPasswordTokenRepository.deleteByUser(u);
            totalInitialPasswordTokens++;
        }
        if (totalInitialPasswordTokens > 0) {
            entityManager.flush();
        }

        List<FactureReelle> facturesReellesForPaiements = factureReelleRepository.findByEntrepriseId(entrepriseId);
        int totalPaiements = 0;
        for (FactureReelle factureReelle : facturesReellesForPaiements) {
            int deleted = entityManager.createNativeQuery(
                "DELETE FROM paiement WHERE facture_reelle_id = :factureReelleId"
            ).setParameter("factureReelleId", factureReelle.getId()).executeUpdate();
            totalPaiements += deleted;
        }
        if (totalPaiements > 0) {
            entityManager.flush();
        }

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

        List<FactureProForma> facturesProformaForNotes = factureProformaRepository.findByEntrepriseId(entrepriseId);
        int totalNotes = 0;
        for (FactureProForma facture : facturesProformaForNotes) {
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

        List<FactureProForma> facturesProformaForHistoriques = factureProformaRepository.findByEntrepriseId(entrepriseId);
        int totalFactProHistoriquesActions = 0;
        for (FactureProForma facture : facturesProformaForHistoriques) {
            factProHistoriqueActionRepository.deleteByFacture(facture);
            totalFactProHistoriquesActions++;
        }
        if (totalFactProHistoriquesActions > 0) {
            entityManager.flush();
        }

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

        List<Vente> ventesWithReferences = venteRepository.findAllByEntrepriseId(entrepriseId);
        
        int totalHistoriques = 0;
        int[] totalFacturesVente = {0};
        for (Vente vente : ventesWithReferences) {
            List<VenteHistorique> historiques = venteHistoriqueRepository.findByVenteId(vente.getId());
            if (!historiques.isEmpty()) {
                totalHistoriques += historiques.size();
                venteHistoriqueRepository.deleteAll(historiques);
            }
            
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
        
        List<FactureVente> facturesVenteRestantes = factureVenteRepository.findAllByEntrepriseId(entrepriseId);
        if (!facturesVenteRestantes.isEmpty()) {
            factureVenteRepository.deleteAll(facturesVenteRestantes);
        }
        
        entityManager.flush();
        
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
        
        if (!ventesWithReferences.isEmpty()) {
            venteRepository.deleteAll(ventesWithReferences);
            entityManager.flush();
        }

        List<Boutique> boutiquesForCaisses = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForCaisses) {
            List<Caisse> caisses = caisseRepository.findByBoutiqueId(boutique.getId());
            if (!caisses.isEmpty()) {
                caisseRepository.deleteAll(caisses);
            }
        }

        entityManager.createNativeQuery(
                "DELETE us FROM user_sessions us " +
                "INNER JOIN user u ON us.user_id = u.id " +
                "WHERE u.entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        entityManager.flush();

        List<User> users = usersRepository.findByEntrepriseId(entrepriseId);
        for (User utilisateur : users) {
            if (utilisateur.getQrCodeUrl() != null && !utilisateur.getQrCodeUrl().isBlank()) {
                try {
                    imageStorageService.deleteQrCodeImage(utilisateur.getQrCodeUrl());
                } catch (Exception e) {
                }
            }
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

        List<FactureReelle> facturesReelles = factureReelleRepository.findByEntrepriseId(entrepriseId);
        factureReelleRepository.deleteAll(facturesReelles);
        entityManager.flush();


        List<FactureProForma> facturesProforma = factureProformaRepository.findByEntrepriseId(entrepriseId);
        factureProformaRepository.deleteAll(facturesProforma);
        entityManager.flush();

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

        Long countClientsRestants = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM client WHERE entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        if (countClientsRestants > 0) {
            entityManager.createNativeQuery(
                "DELETE FROM client WHERE entreprise_id = :entrepriseId"
            ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            entityManager.flush();
        }

        List<EntrepriseClient> entreprisesClients = entrepriseClientRepository.findByEntrepriseId(entrepriseId);
        if (!entreprisesClients.isEmpty()) {
            entrepriseClientRepository.deleteAll(entreprisesClients);
            entityManager.flush();
        }
        
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

        List<Boutique> boutiquesForStockProduitFournisseur = boutiqueRepository.findByEntrepriseId(entrepriseId);
        int totalStockProduitFournisseur = 0;
        for (Boutique boutique : boutiquesForStockProduitFournisseur) {
            List<Stock> stocks = stockRepository.findByBoutiqueId(boutique.getId());
            for (Stock stock : stocks) {
                int deleted = entityManager.createNativeQuery(
                    "DELETE FROM stock_produit_fournisseur WHERE stock_id = :stockId"
                ).setParameter("stockId", stock.getId()).executeUpdate();
                totalStockProduitFournisseur += deleted;
            }
        }
        if (totalStockProduitFournisseur > 0) {
            entityManager.flush();
        }

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


        int totalInteractionsProduit = entityManager.createNativeQuery(
            "DELETE i FROM interactions i " +
            "INNER JOIN produit p ON i.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND i.produit_id IS NOT NULL"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalInteractionsProduit > 0) {
            entityManager.flush();
        }

        int totalProspectAchatsProduit = entityManager.createNativeQuery(
            "DELETE pa FROM prospect_achat pa " +
            "INNER JOIN produit p ON pa.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND pa.produit_id IS NOT NULL"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalProspectAchatsProduit > 0) {
            entityManager.flush();
        }

        List<Produit> produits = produitRepository.findByEntrepriseId(entrepriseId);
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
        

        Long countProduitsRestants = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM produit p " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        if (countProduitsRestants > 0) {
            entityManager.createNativeQuery(
                "DELETE p FROM produit p " +
                "INNER JOIN boutique b ON p.boutique_id = b.id " +
                "WHERE b.entreprise_id = :entrepriseId"
            ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            entityManager.flush();
        }

        List<Categorie> categories = categorieRepository.findByEntrepriseId(entrepriseId);
        categorieRepository.deleteAll(categories);

        List<Unite> unites = uniteRepository.findByEntrepriseId(entrepriseId);
        uniteRepository.deleteAll(unites);

        List<CategorieDepense> categoriesDepenses = categorieDepenseRepository.findByEntrepriseId(entrepriseId);
        categorieDepenseRepository.deleteAll(categoriesDepenses);

        List<Prospect> prospectsForInteractions = prospectRepository.findByEntrepriseId(entrepriseId, Pageable.unpaged()).getContent();
        int totalInteractions = 0;
        int totalAchats = 0;
        for (Prospect prospect : prospectsForInteractions) {
            List<Interaction> interactions = interactionRepository.findByProspectIdOrderByOccurredAtDesc(prospect.getId());
            if (!interactions.isEmpty()) {
                totalInteractions += interactions.size();
                interactionRepository.deleteAll(interactions);
            }
            
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

        List<Prospect> prospects = prospectRepository.findByEntrepriseId(entrepriseId, Pageable.unpaged()).getContent();
        prospectRepository.deleteAll(prospects);
        entityManager.flush();

        List<PaiementModule> paiementsModules = paiementModuleRepository.findByEntrepriseId(entrepriseId);
        paiementModuleRepository.deleteAll(paiementsModules);

        List<EntrepriseModuleEssai> essaisModules = entrepriseModuleEssaiRepository.findByEntreprise(entreprise);
        entrepriseModuleEssaiRepository.deleteAll(essaisModules);

        List<EntrepriseModuleAbonnement> abonnementsModules = entrepriseModuleAbonnementRepository.findByEntrepriseAndActifTrue(entreprise); 
        entrepriseModuleAbonnementRepository.deleteAll(abonnementsModules);


        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        boutiqueRepository.deleteAll(boutiques);
        entityManager.flush();

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

        entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        if (entreprise != null) {
            if (entreprise.getModulesActifs() != null && !entreprise.getModulesActifs().isEmpty()) {
                entreprise.getModulesActifs().clear();
                entrepriseRepository.save(entreprise);
            }
        }

        entityManager.flush();
        entityManager.clear();

        entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        if (entreprise != null) {
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
            
            entrepriseRepository.save(entreprise);
            
            entityManager.flush();
            
            entrepriseRepository.delete(entreprise);
            
            entityManager.flush();
        } else {
        }

    }

    /**
     * Supprime toutes les données de l'entreprise mais garde l'admin et l'entreprise.
     * 
     * Cette méthode supprime toutes les données associées à l'entreprise :
     * - Tous les utilisateurs SAUF l'admin
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteEntrepriseDataButKeepAdmin(User user) {
        if (user.getRole() == null || user.getRole().getName() != RoleType.ADMIN) {
            throw new RuntimeException("Accès refusé : vous devez être administrateur pour vider votre entreprise.");
        }

        Long adminId = user.getId();
        User admin = user;

        Entreprise entreprise = admin.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'admin n'a pas d'entreprise associée.");
        }

        Long entrepriseId = entreprise.getId();

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

        entreprise.setAdmin(null);
        entrepriseRepository.save(entreprise);

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

        List<TransfertFonds> transferts = transfertFondsRepository.findByEntrepriseIdOrderByDateTransfertDesc(entrepriseId);
        transfertFondsRepository.deleteAll(transferts);

        List<User> usersForNotifications = usersRepository.findByEntrepriseId(entrepriseId);
        for (User u : usersForNotifications) {
            List<GlobalNotification> notifications = globalNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(u.getId());
            if (!notifications.isEmpty()) {
                globalNotificationRepository.deleteAll(notifications);
            }
        }

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

        List<Facture> factures = factureRepository.findAllByEntrepriseId(entrepriseId);
        factureRepository.deleteAll(factures);
        entityManager.flush();

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

        List<Boutique> boutiquesForVersements = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForVersements) {
            List<VersementComptable> versements = versementComptableRepository.findByCaisse_BoutiqueId(boutique.getId());
            if (!versements.isEmpty()) {
                versementComptableRepository.deleteAll(versements);
            }
        }

        for (User u : usersForNotifications) {
            List<UserBoutique> userBoutiques = userBoutiqueRepository.findByUserId(u.getId());
            if (!userBoutiques.isEmpty()) {
                userBoutiqueRepository.deleteAll(userBoutiques);
            }
        }

        for (User u : usersForNotifications) {
            passwordResetTokenRepository.deleteByUser(u);
        }
        entityManager.flush();

        for (User u : usersForNotifications) {
            initialPasswordTokenRepository.deleteByUser(u);
        }
        entityManager.flush();

        List<FactureReelle> facturesReellesForPaiements = factureReelleRepository.findByEntrepriseId(entrepriseId);
        for (FactureReelle factureReelle : facturesReellesForPaiements) {
            int deleted = entityManager.createNativeQuery(
                "DELETE FROM paiement WHERE facture_reelle_id = :factureReelleId"
            ).setParameter("factureReelleId", factureReelle.getId()).executeUpdate();
        }
        entityManager.flush();

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

        List<FactureProForma> facturesProformaForNotes = factureProformaRepository.findByEntrepriseId(entrepriseId);
        for (FactureProForma facture : facturesProformaForNotes) {
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

        List<FactureProForma> facturesProformaForHistoriques = factureProformaRepository.findByEntrepriseId(entrepriseId);
        for (FactureProForma facture : facturesProformaForHistoriques) {
            factProHistoriqueActionRepository.deleteByFacture(facture);
        }
        entityManager.flush();

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

        List<Vente> ventesWithReferences = venteRepository.findAllByEntrepriseId(entrepriseId);
        
        int totalHistoriques = 0;
        int[] totalFacturesVente = {0};
        for (Vente vente : ventesWithReferences) {
            List<VenteHistorique> historiques = venteHistoriqueRepository.findByVenteId(vente.getId());
            if (!historiques.isEmpty()) {
                totalHistoriques += historiques.size();
                venteHistoriqueRepository.deleteAll(historiques);
            }
            
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
        
        if (!ventesWithReferences.isEmpty()) {
            venteRepository.deleteAll(ventesWithReferences);
            entityManager.flush();
        }

        List<Boutique> boutiquesForCaisses = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForCaisses) {
            List<Caisse> caisses = caisseRepository.findByBoutiqueId(boutique.getId());
            if (!caisses.isEmpty()) {
                caisseRepository.deleteAll(caisses);
            }
        }


        entityManager.createNativeQuery(
                "DELETE us FROM user_sessions us " +
                "INNER JOIN user u ON us.user_id = u.id " +
                "WHERE u.entreprise_id = :entrepriseId AND u.id != :adminId"
        ).setParameter("entrepriseId", entrepriseId)
         .setParameter("adminId", adminId)
         .executeUpdate();
        entityManager.flush();

        List<User> users = usersRepository.findByEntrepriseId(entrepriseId);
        for (User utilisateur : users) {
            if (utilisateur.getId().equals(adminId)) {
                continue;
            }
            
            if (utilisateur.getQrCodeUrl() != null && !utilisateur.getQrCodeUrl().isBlank()) {
                try {
                    imageStorageService.deleteQrCodeImage(utilisateur.getQrCodeUrl());
                } catch (Exception e) {
                }
            }
            if (utilisateur.getPhoto() != null && !utilisateur.getPhoto().isBlank()) {
                try {
                    Path photoPath = Paths.get("src/main/resources/static" + utilisateur.getPhoto());
                    Files.deleteIfExists(photoPath);
                } catch (IOException e) {
                }
            }
        }
        users.removeIf(u -> u.getId().equals(adminId));
        if (!users.isEmpty()) {
            usersRepository.deleteAll(users);
            entityManager.flush();
        }

        List<FactureReelle> facturesReelles = factureReelleRepository.findByEntrepriseId(entrepriseId);
        factureReelleRepository.deleteAll(facturesReelles);
        entityManager.flush();

        List<FactureProForma> facturesProforma = factureProformaRepository.findByEntrepriseId(entrepriseId);
        factureProformaRepository.deleteAll(facturesProforma);
        entityManager.flush();

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
        
        Long countClientsRestants = (Long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM client WHERE entreprise_id = :entrepriseId"
        ).setParameter("entrepriseId", entrepriseId).getSingleResult();
        
        if (countClientsRestants > 0) {
            entityManager.createNativeQuery(
                "DELETE FROM client WHERE entreprise_id = :entrepriseId"
            ).setParameter("entrepriseId", entrepriseId).executeUpdate();
            entityManager.flush();
        }

        List<EntrepriseClient> entreprisesClients = entrepriseClientRepository.findByEntrepriseId(entrepriseId);
        if (!entreprisesClients.isEmpty()) {
            entrepriseClientRepository.deleteAll(entreprisesClients);
            entityManager.flush();
        }
        
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

        List<Boutique> boutiquesForStocks = boutiqueRepository.findByEntrepriseId(entrepriseId);
        for (Boutique boutique : boutiquesForStocks) {
            List<Stock> stocks = stockRepository.findByBoutiqueId(boutique.getId());
            if (!stocks.isEmpty()) {
                stockRepository.deleteAll(stocks);
            }
        }
        entityManager.flush();

        int totalInteractionsProduit = entityManager.createNativeQuery(
            "DELETE i FROM interactions i " +
            "INNER JOIN produit p ON i.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND i.produit_id IS NOT NULL"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalInteractionsProduit > 0) {
            entityManager.flush();
        }

        int totalProspectAchatsProduit = entityManager.createNativeQuery(
            "DELETE pa FROM prospect_achat pa " +
            "INNER JOIN produit p ON pa.produit_id = p.id " +
            "INNER JOIN boutique b ON p.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND pa.produit_id IS NOT NULL"
        ).setParameter("entrepriseId", entrepriseId).executeUpdate();
        if (totalProspectAchatsProduit > 0) {
            entityManager.flush();
        }

 
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

        List<Categorie> categories = categorieRepository.findByEntrepriseId(entrepriseId);
        categorieRepository.deleteAll(categories);

        List<Unite> unites = uniteRepository.findByEntrepriseId(entrepriseId);
        uniteRepository.deleteAll(unites);

        List<CategorieDepense> categoriesDepenses = categorieDepenseRepository.findByEntrepriseId(entrepriseId);
        categorieDepenseRepository.deleteAll(categoriesDepenses);

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
            
            
            List<Facture> facturesPremiereBoutique = factureRepository.findByBoutiqueIdAndEntrepriseId(
                    premiereBoutiqueId, entrepriseId);
            for (Facture facture : facturesPremiereBoutique) {
                entityManager.createNativeQuery(
                    "DELETE FROM facture_produit WHERE facture_id = :factureId"
                ).setParameter("factureId", facture.getId()).executeUpdate();
            }
            entityManager.flush();
            
            factureRepository.deleteAll(facturesPremiereBoutique);
            entityManager.flush();
            
            List<Caisse> caissesPremiereBoutique = caisseRepository.findByBoutiqueId(premiereBoutiqueId);
            for (Caisse caisse : caissesPremiereBoutique) {
                List<MouvementCaisse> mouvements = mouvementCaisseRepository.findByCaisseId(caisse.getId());
                if (!mouvements.isEmpty()) {
                    mouvementCaisseRepository.deleteAll(mouvements);
                }
            }
            entityManager.flush();
            
            List<VersementComptable> versementsPremiereBoutique = versementComptableRepository.findByCaisse_BoutiqueId(premiereBoutiqueId);
            if (!versementsPremiereBoutique.isEmpty()) {
                versementComptableRepository.deleteAll(versementsPremiereBoutique);
            }
            entityManager.flush();
            
            if (!caissesPremiereBoutique.isEmpty()) {
                caisseRepository.deleteAll(caissesPremiereBoutique);
            }
            entityManager.flush();
            
            List<UserBoutique> userBoutiquesPremiere = userBoutiqueRepository.findByBoutiqueId(premiereBoutiqueId);
            if (!userBoutiquesPremiere.isEmpty()) {
                userBoutiqueRepository.deleteAll(userBoutiquesPremiere);
            }
            entityManager.flush();
            
            List<Stock> stocksPremiereBoutique = stockRepository.findByBoutiqueId(premiereBoutiqueId);
            for (Stock stock : stocksPremiereBoutique) {
                entityManager.createNativeQuery(
                    "DELETE FROM stock_produit_fournisseur WHERE stock_id = :stockId"
                ).setParameter("stockId", stock.getId()).executeUpdate();
            }
            entityManager.flush();
            
            if (!stocksPremiereBoutique.isEmpty()) {
                stockRepository.deleteAll(stocksPremiereBoutique);
            }
            entityManager.flush();
            
            int interactionsPremiereBoutique = entityManager.createNativeQuery(
                "DELETE i FROM interactions i " +
                "INNER JOIN produit p ON i.produit_id = p.id " +
                "WHERE p.boutique_id = :boutiqueId AND i.produit_id IS NOT NULL"
            ).setParameter("boutiqueId", premiereBoutiqueId).executeUpdate();
            if (interactionsPremiereBoutique > 0) {
                entityManager.flush();
            }
            
            int prospectAchatsPremiereBoutique = entityManager.createNativeQuery(
                "DELETE pa FROM prospect_achat pa " +
                "INNER JOIN produit p ON pa.produit_id = p.id " +
                "WHERE p.boutique_id = :boutiqueId AND pa.produit_id IS NOT NULL"
            ).setParameter("boutiqueId", premiereBoutiqueId).executeUpdate();
            if (prospectAchatsPremiereBoutique > 0) {
                entityManager.flush();
            }
            

            
            List<Vente> ventesPremiereBoutique = venteRepository.findByBoutiqueId(premiereBoutiqueId);
            for (Vente vente : ventesPremiereBoutique) {
                List<VenteHistorique> historiques = venteHistoriqueRepository.findByVenteId(vente.getId());
                if (!historiques.isEmpty()) {
                    venteHistoriqueRepository.deleteAll(historiques);
                }
                
                Long venteEntrepriseId = vente.getBoutique() != null && vente.getBoutique().getEntreprise() != null 
                        ? vente.getBoutique().getEntreprise().getId() : null;
                if (venteEntrepriseId != null) {
                    factureVenteRepository.findByVenteIdAndEntrepriseId(vente.getId(), venteEntrepriseId)
                            .ifPresent(factureVenteRepository::delete);
                }
            }
            entityManager.flush();
            
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
            
            List<Boutique> autresBoutiques = boutiques.stream()
                    .filter(b -> !b.getId().equals(premiereBoutiqueId))
                    .collect(Collectors.toList());
            
            if (!autresBoutiques.isEmpty()) {
                boutiqueRepository.deleteAll(autresBoutiques);
            }
            entityManager.flush();
        } else {
            if (!boutiques.isEmpty()) {
                boutiqueRepository.deleteAll(boutiques);
                entityManager.flush();
            }
        }

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

        entityManager.flush();
        entityManager.clear();

        entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
        if (entreprise != null) {
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
     */
    @Transactional(rollbackFor = Exception.class)
    public void deconnecterTousLesUtilisateurs(User superAdmin) {
        ensureSuperAdmin(superAdmin);

   
        List<com.xpertcash.entity.UserSession> allSessions = userSessionRepository.findAll();
        if (!allSessions.isEmpty()) {
            userSessionRepository.deleteAll(allSessions);
            entityManager.flush();
        }


        List<User> allUsers = usersRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (User user : allUsers) {
            user.setLastActivity(now);
            usersRepository.save(user);
        }
        entityManager.flush();

    
    }
}



package com.xpertcash.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.SuperAdminEntrepriseListDTO;
import com.xpertcash.DTOs.SuperAdminEntrepriseStatsDTO;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.VENTE.StatutCaisse;
import com.xpertcash.entity.User;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.PROSPECT.ProspectRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.VENTE.CaisseRepository;
import com.xpertcash.repository.VENTE.VenteRepository;

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
}



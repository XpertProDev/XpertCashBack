package com.xpertcash.service.Module;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.DTOs.Module.ModuleDTO;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.entity.Module.EntrepriseModuleAbonnement;
import com.xpertcash.entity.Module.EntrepriseModuleEssai;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.Module.EntrepriseModuleAbonnementRepository;
import com.xpertcash.repository.Module.EntrepriseModuleEssaiRepository;
import com.xpertcash.repository.Module.ModuleRepository;
import com.xpertcash.service.MailService;

import jakarta.servlet.http.HttpServletRequest;
import com.xpertcash.service.AuthenticationHelper;

@Service
public class ModuleActivationService {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ModulePaiementService modulePaiementService;

    @Autowired
    private MailService mailService;

    @Autowired
    private EntrepriseModuleEssaiRepository entrepriseModuleEssaiRepository;

    @Autowired
    private EntrepriseModuleAbonnementRepository entrepriseModuleAbonnementRepository;

    // Méthode réutilisable
public boolean isModuleActifPourEntreprise(Entreprise entreprise, String codeModule) {

    Optional<AppModule> moduleOpt = moduleRepository.findByCode(codeModule);
    if (moduleOpt.isEmpty()) return false;

    AppModule module = moduleOpt.get();

    Set<AppModule> modulesAchetes = entreprise.getModulesActifs();
    boolean moduleAchete = modulesAchetes.contains(module);

    if (!module.isPayant()) {
        return moduleAchete;
    }

    Optional<EntrepriseModuleAbonnement> abonnementOpt = entrepriseModuleAbonnementRepository
            .findByEntrepriseAndModuleAndActifTrue(entreprise, module);

    boolean abonnementValide = abonnementOpt.isPresent() &&
            abonnementOpt.get().getDateFin() != null &&
            abonnementOpt.get().getDateFin().isAfter(LocalDateTime.now());

    if (moduleAchete && !abonnementValide) {
        entreprise.getModulesActifs().remove(module);
        entrepriseRepository.save(entreprise);

        abonnementOpt.ifPresent(abonnement -> {
            abonnement.setActif(false);
            entrepriseModuleAbonnementRepository.save(abonnement);
        });

        moduleAchete = false;
    }

    Optional<EntrepriseModuleEssai> essaiOpt = entrepriseModuleEssaiRepository.findByEntrepriseAndModule(entreprise, module);

    boolean essaiValide = essaiOpt.isPresent() &&
                          essaiOpt.get().getDateFinEssai() != null &&
                          essaiOpt.get().getDateFinEssai().isAfter(LocalDateTime.now());

    return moduleAchete || essaiValide;
}

 
    public void verifierAccesModulePourEntreprise(Entreprise entreprise, String codeModule) {

        if (isModuleActifPourEntreprise(entreprise, codeModule)) {
            return;
        }

        Map<String, String> tempsRestantParModule = consulterTempsRestantEssaiParModule(entreprise);
        String tempsRestant = tempsRestantParModule.getOrDefault(codeModule, "Période d'essai terminée");

        if ("Période d'essai terminée".equals(tempsRestant)) {
            throw new RuntimeException("Ce module n'est pas activé pour votre entreprise. Veuillez l'acheter pour continuer.");
        } else {
            throw new RuntimeException("Ce module n'est pas activé pour votre entreprise. Temps d'essai restant : " + tempsRestant);
        }
    }


    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void synchroniserAbonnementsExpirés() {

        List<EntrepriseModuleAbonnement> abonnementsExpirés = entrepriseModuleAbonnementRepository
                .findByActifTrueAndDateFinBefore(LocalDateTime.now());

        System.out.println("Nombre d'abonnements expirés détectés : " + abonnementsExpirés.size());

        for (EntrepriseModuleAbonnement abo : abonnementsExpirés) {

            abo.setActif(false);
            entrepriseModuleAbonnementRepository.save(abo);

            Entreprise entreprise = abo.getEntreprise();
            AppModule module = abo.getModule();

            if (entreprise.getModulesActifs().contains(module)) {
                entreprise.getModulesActifs().remove(module);
                entrepriseRepository.save(entreprise);

                System.out.println("Module '" + module.getNom() + "' retiré de l'entreprise '" 
                    + entreprise.getNomEntreprise() + "' (ID entreprise : " + entreprise.getId() + ")");
            } else {
                System.out.println("Module '" + module.getNom() + "' déjà non actif pour l'entreprise '" 
                    + entreprise.getNomEntreprise() + "'");
            }
        }

        System.out.println("Synchronisation des abonnements expirés terminée.");
    }



    private static final List<String> SOUS_MODULES_MASQUES = List.of(
        "FACTURE_PROFORMA",
        "FACTURE_REELLE"
    );




    public void initialiserEssaisModulesPayants(Entreprise entreprise) {

    List<AppModule> modulesPayants = moduleRepository.findByPayantTrue();
    LocalDateTime now = LocalDateTime.now();

    List<EntrepriseModuleEssai> essais = new ArrayList<>();

    for (AppModule module : modulesPayants) {
        
        if (entrepriseModuleEssaiRepository.existsByEntrepriseAndModule(entreprise, module)) {
            continue; 
        }

        EntrepriseModuleEssai essai = new EntrepriseModuleEssai();
        essai.setEntreprise(entreprise);
        essai.setModule(module);
        essai.setDateDebutEssai(now);
        essai.setDateFinEssai(now.plusDays(365));
        essais.add(essai);
    }

    if (!essais.isEmpty()) {
        entrepriseModuleEssaiRepository.saveAll(essais);
    }
}

    //Definir prix des modules
    @Transactional
    public void mettreAJourPrixModule(String codeModule, BigDecimal nouveauPrix) {

        AppModule module = moduleRepository.findByCode(codeModule)
                .orElseThrow(() -> new RuntimeException("Module introuvable avec le code : " + codeModule));

        module.setPrix(nouveauPrix);
        moduleRepository.save(module);
    }


    //Consulter le temps restant de la période d'essai pour les modules payants
    public Map<String, String> consulterTempsRestantEssaiParModule(Entreprise entreprise) {
    List<EntrepriseModuleEssai> essais = entrepriseModuleEssaiRepository.findByEntreprise(entreprise);

    Map<String, String> tempsRestantParModule = new HashMap<>();
    LocalDateTime maintenant = LocalDateTime.now();

    for (EntrepriseModuleEssai essai : essais) {
        if (essai.getDateFinEssai() == null || essai.getDateFinEssai().isBefore(maintenant)) {
            tempsRestantParModule.put(essai.getModule().getCode(), "Terminé");
        } else {
            Duration dureeRestante = Duration.between(maintenant, essai.getDateFinEssai());

            long jours = dureeRestante.toDays();
            long heures = dureeRestante.toHours() % 24;
            long minutes = dureeRestante.toMinutes() % 60;

            String temps;

            if (jours > 0) {
                temps = String.format("%dj %dh", jours, heures);
            } else if (heures > 0) {
                temps = String.format("%dh%02d", heures, minutes);
            } else {
                temps = String.format("%dmin", minutes);
            }

            tempsRestantParModule.put(essai.getModule().getCode(), temps);
        }
    }

    return tempsRestantParModule;
}


    //Activation d'un module pour une entreprise
    @Transactional
public void activerModuleAvecPaiement(Long userId,
                                      String nomModule,
                                      int dureeMois,
                                      String numeroCarte,
                                      String cvc,
                                      String dateExpiration,
                                      String nomCompletProprietaire,
                                      String emailProprietaireCarte,
                                      String pays,
                                      String adresse,
                                      String ville) {

    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Utilisateur non associé à une entreprise");
    }

    RoleType role = user.getRole().getName();
    if (role != RoleType.ADMIN) {
        throw new RuntimeException("Seuls l'ADMIN peuvent activer les modules.");
    }

    AppModule module = moduleRepository.findByNom(nomModule)
            .orElseThrow(() -> new RuntimeException("Module '" + nomModule + "' introuvable"));

    if (entreprise.getModulesActifs().contains(module)) {
        throw new RuntimeException("Ce module est déjà activé pour cette entreprise.");
    }

    String referenceTransaction = null;
    BigDecimal montant = BigDecimal.ZERO;
    BigDecimal prixUnitaire = BigDecimal.ZERO;

    if (module.isPayant()) {
        prixUnitaire = module.getPrix();
        if (prixUnitaire == null || prixUnitaire.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Prix du module invalide");
        }

        montant = prixUnitaire.multiply(BigDecimal.valueOf(dureeMois));

        if (numeroCarte == null || numeroCarte.isBlank() ||
            cvc == null || cvc.isBlank() ||
            dateExpiration == null || dateExpiration.isBlank() ||
            nomCompletProprietaire == null || nomCompletProprietaire.isBlank() ||
            emailProprietaireCarte == null || emailProprietaireCarte.isBlank() ||
            adresse == null || adresse.isBlank() ||
            ville == null || ville.isBlank()) {
            throw new RuntimeException("Toutes les informations de paiement et du propriétaire sont requises.");
        }

        boolean paiementReussi = modulePaiementService.effectuerPaiement(
            numeroCarte,
            cvc,
            dateExpiration,
            montant,
            entreprise,
            module,
            nomCompletProprietaire,
            emailProprietaireCarte,
            pays,
            adresse,
            ville
        );

        if (!paiementReussi) {
            throw new RuntimeException("Échec du paiement. Activation annulée.");
        }

        referenceTransaction = modulePaiementService.enregistrerFacturePaiement(
            entreprise,
            module,
            montant,
            nomCompletProprietaire,
            emailProprietaireCarte,
            pays,
            adresse,
            ville
        );
    }

    // Vérifier si un abonnement inactif existe déjà
    Optional<EntrepriseModuleAbonnement> abonnementInactifOpt = entrepriseModuleAbonnementRepository
            .findTopByEntrepriseAndModuleOrderByDateFinDesc(entreprise, module);

    if (abonnementInactifOpt.isPresent() && !abonnementInactifOpt.get().isActif()) {

        EntrepriseModuleAbonnement ancienAbonnement = abonnementInactifOpt.get();
        ancienAbonnement.setActif(true);
        ancienAbonnement.setDateDebut(LocalDateTime.now());
        ancienAbonnement.setDateFin(LocalDateTime.now().plusMonths(dureeMois));

        entrepriseModuleAbonnementRepository.save(ancienAbonnement);

    } else {
        EntrepriseModuleAbonnement nouvelAbonnement = new EntrepriseModuleAbonnement();
        nouvelAbonnement.setEntreprise(entreprise);
        nouvelAbonnement.setModule(module);
        nouvelAbonnement.setDateDebut(LocalDateTime.now());
        nouvelAbonnement.setDateFin(LocalDateTime.now().plusMonths(dureeMois));
        nouvelAbonnement.setActif(true);

        entrepriseModuleAbonnementRepository.save(nouvelAbonnement);
    }

    entreprise.getModulesActifs().add(module);
    entrepriseRepository.save(entreprise);

    try {
        mailService.sendConfirmationActivationEmail(
            emailProprietaireCarte,
            module.getNom(),
            prixUnitaire,
            montant,
            "XOF",
            nomCompletProprietaire,
            pays,
            adresse,
            ville,
            referenceTransaction,
            entreprise.getNomEntreprise(),
            dureeMois
        );
    } catch (Exception e) {
        System.err.println("Échec d'envoi de l'email de confirmation : " + e.getMessage());
    }
}



   //Lister tout les modules actifs ou non actifs pour une entreprise que par son admin ou manager
    public List<ModuleDTO> listerModulesEntreprise(HttpServletRequest request) {

    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token manquant ou mal formé");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    if (user.getEntreprise() == null) {
        throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
    }

    // Vérification des droits
    // RoleType role = user.getRole().getName();
    // if (!(role == RoleType.ADMIN || role == RoleType.MANAGER)) {
    //     throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires.");
    // }

    if (user.getEntreprise() == null) {
        throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
    }

    Entreprise entreprise = user.getEntreprise();
    List<AppModule> tousModules = moduleRepository.findAll();

    //  Récupération globale des temps restants par module
    Map<String, String> tempsRestantParModule = consulterTempsRestantEssaiParModule(entreprise);

    // Transformation de la liste avec filtrage des sous-modules et ajout du temps d'essai
    List<ModuleDTO> modules = tousModules.stream()
            .filter(module -> !SOUS_MODULES_MASQUES.contains(module.getCode()))
            .map(module -> {

                boolean actifReel = isModuleActifPourEntreprise(entreprise, module.getCode());

                String tempsRestant = null;
                
                // Affichage du temps restant uniquement si le module est payant et non encore acheté
                if (module.isPayant() && !entreprise.getModulesActifs().contains(module)) {
                    tempsRestant = tempsRestantParModule.get(module.getCode());
                }

                return new ModuleDTO(
                        module.getId(),
                        module.getNom(),
                        module.getCode(),
                        module.getDescription(),
                        module.isPayant(),
                        actifReel,
                        module.getPrix(),
                        tempsRestant
                );
            })
            .collect(Collectors.toList());

    return modules;
}

// Activation automatique de l'essai pour entreprises existantes
    public void activerEssaiPourEntreprise(Entreprise entreprise, AppModule module) {
    if (dejaEssaiPourEntreprise(entreprise, module)) {
        return; // Si l'essai existe déjà, on ne fait rien
    }

    LocalDateTime now = LocalDateTime.now();

    EntrepriseModuleEssai essai = new EntrepriseModuleEssai();
    essai.setEntreprise(entreprise);
    essai.setModule(module);
    essai.setDateDebutEssai(now);
    essai.setDateFinEssai(now.plusDays(365));

    entrepriseModuleEssaiRepository.save(essai);
}


public boolean dejaEssaiPourEntreprise(Entreprise entreprise, AppModule module) {
    return entrepriseModuleEssaiRepository.findByEntrepriseAndModule(entreprise, module).isPresent();
}

// Consulter les abonnements actifs d'une entreprise
public List<Map<String, Object>> consulterAbonnementsActifs(Entreprise entreprise) {
    List<EntrepriseModuleAbonnement> abonnements = entrepriseModuleAbonnementRepository.findByEntrepriseAndActifTrue(entreprise);
    
    return abonnements.stream()
        .map(abonnement -> {
            Map<String, Object> abonnementData = new HashMap<>();
            abonnementData.put("id", abonnement.getId());
            abonnementData.put("moduleCode", abonnement.getModule().getCode());
            abonnementData.put("moduleNom", abonnement.getModule().getNom());
            abonnementData.put("dateDebut", abonnement.getDateDebut());
            abonnementData.put("dateFin", abonnement.getDateFin());
            abonnementData.put("actif", abonnement.isActif());
            abonnementData.put("joursRestants", calculerJoursRestants(abonnement.getDateFin()));
            return abonnementData;
        })
        .collect(Collectors.toList());
}

// Calculer les jours restants avant expiration
private long calculerJoursRestants(LocalDateTime dateFin) {
    if (dateFin == null) return -1; 
    
    LocalDateTime maintenant = LocalDateTime.now();
    if (dateFin.isBefore(maintenant)) return 0;
    
    return java.time.Duration.between(maintenant, dateFin).toDays();
}

// Désactiver manuellement un module pour une entreprise
@Transactional
public boolean desactiverModulePourEntreprise(Entreprise entreprise, String codeModule) {
    Optional<AppModule> moduleOpt = moduleRepository.findByCode(codeModule);
    if (moduleOpt.isEmpty()) {
        return false; 
    }
    
    AppModule module = moduleOpt.get();
    
    boolean moduleRetire = entreprise.getModulesActifs().remove(module);
    
    Optional<EntrepriseModuleAbonnement> abonnementOpt = entrepriseModuleAbonnementRepository
            .findByEntrepriseAndModuleAndActifTrue(entreprise, module);
    
    if (abonnementOpt.isPresent()) {
        EntrepriseModuleAbonnement abonnement = abonnementOpt.get();
        abonnement.setActif(false);
        entrepriseModuleAbonnementRepository.save(abonnement);
    }
    
    if (moduleRetire) {
        entrepriseRepository.save(entreprise);
    }
    
    return moduleRetire;
}

}

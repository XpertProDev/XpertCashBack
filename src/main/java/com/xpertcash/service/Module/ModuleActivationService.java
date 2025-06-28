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
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.Module.ModuleDTO;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.entity.Module.EntrepriseModuleEssai;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.Module.EntrepriseModuleEssaiRepository;
import com.xpertcash.repository.Module.ModuleRepository;
import com.xpertcash.service.MailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class ModuleActivationService {

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private UsersRepository usersRepository;

     @Autowired
    private JwtUtil jwtUtil; 

    @Autowired
    private ModulePaiementService modulePaiementService;

    @Autowired
    private MailService mailService;

    @Autowired
    private EntrepriseModuleEssaiRepository entrepriseModuleEssaiRepository;

    //Methode reetulisable
        public boolean isModuleActifPourEntreprise(Entreprise entreprise, String codeModule) {
        Optional<AppModule> moduleOpt = moduleRepository.findByCode(codeModule);
        if (moduleOpt.isEmpty()) return false;

        AppModule module = moduleOpt.get();

        boolean moduleAchete = entreprise.getModulesActifs().contains(module);

        if (!module.isPayant()) {
            return moduleAchete;
        }

        // Pour les modules payants : vérification essai individuel
        Optional<EntrepriseModuleEssai> essaiOpt = entrepriseModuleEssaiRepository.findByEntrepriseAndModule(entreprise, module);

        boolean essaiValide = essaiOpt.isPresent() &&
                            essaiOpt.get().getDateFinEssai() != null &&
                            essaiOpt.get().getDateFinEssai().isAfter(LocalDateTime.now());

        return moduleAchete || essaiValide;
    }

    //Module de verification reutilisable
    public void verifierAccesModulePourEntreprise(Entreprise entreprise, String codeModule) {

        if (isModuleActifPourEntreprise(entreprise, codeModule)) {
            return; // Tout est ok, accès autorisé
        }

        Map<String, String> tempsRestantParModule = consulterTempsRestantEssaiParModule(entreprise);
        String tempsRestant = tempsRestantParModule.getOrDefault(codeModule, "Période d'essai terminée");

        if ("Période d'essai terminée".equals(tempsRestant)) {
            throw new RuntimeException("Ce module n'est pas activé pour votre entreprise. Veuillez l'acheter pour continuer.");
        } else {
            throw new RuntimeException("Ce module n'est pas activé pour votre entreprise. Temps d'essai restant : " + tempsRestant);
        }
    }






    // Initialiser les essais pour les modules payants
    public void initialiserEssaisModulesPayants(Entreprise entreprise) {

    List<AppModule> modulesPayants = moduleRepository.findByPayantTrue();
    LocalDateTime now = LocalDateTime.now();

    List<EntrepriseModuleEssai> essais = new ArrayList<>();

    for (AppModule module : modulesPayants) {
        
        // Évite les doublons si un essai existe déjà
        if (entrepriseModuleEssaiRepository.existsByEntrepriseAndModule(entreprise, module)) {
            continue; 
        }

        EntrepriseModuleEssai essai = new EntrepriseModuleEssai();
        essai.setEntreprise(entreprise);
        essai.setModule(module);
        essai.setDateDebutEssai(now);
        essai.setDateFinEssai(now.plusDays(1));
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
            tempsRestantParModule.put(essai.getModule().getCode(), "Période d'essai terminée");
        } else {
            Duration dureeRestante = Duration.between(maintenant, essai.getDateFinEssai());
            long heures = dureeRestante.toHours();
            long minutes = dureeRestante.toMinutes() % 60;
            tempsRestantParModule.put(essai.getModule().getCode(),
                String.format("%d heures et %d minutes restantes", heures, minutes));
        }
    }

    return tempsRestantParModule;
}





    //Activation d'un module pour une entreprise
    @Transactional
    public void activerModuleAvecPaiement(Long userId,
                                        String nomModule,
                                        String numeroCarte,
                                        String cvc,
                                        String dateExpiration,
                                        String nomProprietaire,
                                        String prenomProprietaire,
                                        String emailProprietaireCarte,
                                        String adresse,
                                        String ville) {

        // 1. Vérification utilisateur et entreprise
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

        // 2. Récupération du module
        AppModule module = moduleRepository.findByNom(nomModule)
                .orElseThrow(() -> new RuntimeException("Module '" + nomModule + "' introuvable"));

        if (entreprise.getModulesActifs().contains(module)) {
            throw new RuntimeException("Ce module est déjà activé pour cette entreprise.");
        }

        String referenceTransaction = null;

        // 3. Si payant, vérifier les infos et procéder au paiement
        if (module.isPayant()) {
            BigDecimal montant = module.getPrix();
            if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Prix du module invalide");
            }

            // Vérification stricte des infos
            if (numeroCarte == null || numeroCarte.isBlank() ||
                cvc == null || cvc.isBlank() ||
                dateExpiration == null || dateExpiration.isBlank() ||
                nomProprietaire == null || nomProprietaire.isBlank() ||
                prenomProprietaire == null || prenomProprietaire.isBlank() ||
                emailProprietaireCarte == null || emailProprietaireCarte.isBlank() ||
                adresse == null || adresse.isBlank() ||
                ville == null || ville.isBlank()) {
                throw new RuntimeException("Toutes les informations de paiement et du propriétaire sont requises.");
            }

            // Paiement simulé
                boolean paiementReussi = modulePaiementService.effectuerPaiement(
                    numeroCarte,
                    cvc,
                    dateExpiration,
                    montant,
                    entreprise,
                    module,
                    nomProprietaire,
                    prenomProprietaire,
                    emailProprietaireCarte,
                    adresse,
                    ville
                );

            if (!paiementReussi) {
                throw new RuntimeException("Échec du paiement. Activation annulée.");
            }

            // Génération de la facture et récupération de la référence
            referenceTransaction = modulePaiementService.enregistrerFacturePaiement(
            entreprise,
            module,
            montant,
            nomProprietaire,
            prenomProprietaire,
            emailProprietaireCarte,
            adresse,
            ville
        );

        }
        // 4. Activation du module
        entreprise.getModulesActifs().add(module);
        entrepriseRepository.save(entreprise);
        
        System.out.println("Envoi de l'email à : " + emailProprietaireCarte);


        // 5. Envoi de l'email de confirmation avec facture
    try {
        mailService.sendConfirmationActivationEmail(
               emailProprietaireCarte,
                module.getNom(),
                module.getPrix(),
                "XOF",
                nomProprietaire,
                prenomProprietaire,
                adresse,
                ville,
                referenceTransaction,
                entreprise.getNomEntreprise()
                
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

    Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    if (user.getEntreprise() == null) {
        throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
    }

    // Vérification des droits
    RoleType role = user.getRole().getName();
    if (!(role == RoleType.ADMIN || role == RoleType.MANAGER)) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires.");
    }

    Entreprise entreprise = user.getEntreprise();

    List<AppModule> tousModules = moduleRepository.findAll();

    List<ModuleDTO> modules = tousModules.stream()
            .map(module -> {

                boolean actifReel = isModuleActifPourEntreprise(entreprise, module.getCode());  // prend en compte essai + achat
            System.out.println("Module " + module.getCode() + " actif pour entreprise : " + actifReel);
                return new ModuleDTO(
                    module.getId(),
                    module.getNom(),
                    module.getCode(),
                    module.getDescription(),
                    module.isPayant(),
                    actifReel,
                    module.getPrix()
                );
            })
            .collect(Collectors.toList());

    return modules;
}

// Activation automatique de l'essai pour entreprises existantes
public void activerEssaiPourToutesLesEntreprises(AppModule module) {
    List<Entreprise> entreprises = entrepriseRepository.findAll();
    LocalDateTime now = LocalDateTime.now();

    List<EntrepriseModuleEssai> essais = entreprises.stream().map(entreprise -> {
        EntrepriseModuleEssai essai = new EntrepriseModuleEssai();
        essai.setEntreprise(entreprise);
        essai.setModule(module);
        essai.setDateDebutEssai(now);
        essai.setDateFinEssai(now.plusDays(1));
        return essai;
    }).toList();

    entrepriseModuleEssaiRepository.saveAll(essais);
}

}
//* Si le module est payant, tu peux gérer ici la vérification du paiement.
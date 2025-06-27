package com.xpertcash.service.Module;

import java.math.BigDecimal;
import java.util.List;
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
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.UsersRepository;
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

    //Methode reetulisable
        public boolean isModuleActifPourEntreprise(Entreprise entreprise, String codeModule) {
        return entreprise.getModulesActifs().stream()
                .anyMatch(module -> codeModule.equalsIgnoreCase(module.getCode()));
        }

    //Definir prix des modules
    @Transactional
    public void mettreAJourPrixModule(String codeModule, BigDecimal nouveauPrix) {

        AppModule module = moduleRepository.findByCode(codeModule)
                .orElseThrow(() -> new RuntimeException("Module introuvable avec le code : " + codeModule));

        module.setPrix(nouveauPrix);
        moduleRepository.save(module);
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
        if (role != RoleType.ADMIN && role != RoleType.MANAGER) {
            throw new RuntimeException("Seuls les ADMIN ou MANAGER peuvent activer les modules.");
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

            // Vérification stricte des infos de carte et propriétaire
            if (numeroCarte == null || numeroCarte.isBlank() ||
                cvc == null || cvc.isBlank() ||
                dateExpiration == null || dateExpiration.isBlank() ||
                nomProprietaire == null || nomProprietaire.isBlank() ||
                prenomProprietaire == null || prenomProprietaire.isBlank() ||
                adresse == null || adresse.isBlank() ||
                ville == null || ville.isBlank()) {
                throw new RuntimeException("Toutes les informations de paiement et du propriétaire sont requises.");
            }

            // Simulation du paiement
            boolean paiementReussi = modulePaiementService.effectuerPaiement(
                    numeroCarte,
                    cvc,
                    dateExpiration,
                    montant,
                    entreprise,
                    module,
                    nomProprietaire,
                    prenomProprietaire,
                    adresse,
                    ville
            );

            if (!paiementReussi) {
                throw new RuntimeException("Échec du paiement. Activation annulée.");
            }

            // Optionnel : Enregistrer les détails du paiement pour la facture
            modulePaiementService.enregistrerFacturePaiement(entreprise, module, montant, nomProprietaire, prenomProprietaire, adresse, ville);
        }

        // 4. Activation du module
        entreprise.getModulesActifs().add(module);
        entrepriseRepository.save(entreprise);

        // 5. Envoi de l'email de confirmation avec facture
    try {
        mailService.sendConfirmationActivationEmail(
                user.getEmail(),
                module.getNom(),
                module.getPrix(),
                "XOF",
                nomProprietaire,
                prenomProprietaire,
                adresse,
                ville,
                referenceTransaction != null ? referenceTransaction : "N/A"
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
    Set<AppModule> modulesActifs = entreprise.getModulesActifs();

    // Récupérer tous les modules existants
    List<AppModule> tousModules = moduleRepository.findAll();

    // Construction de la réponse
    List<ModuleDTO> modules = tousModules.stream()
            .map(module -> {
                boolean actif = modulesActifs.contains(module);
                return new ModuleDTO(module.getId(), module.getNom(), module.getCode(), module.isPayant(), actif, module.getPrix());
            })
            .collect(Collectors.toList());

    return modules;
}

  
}
//* Si le module est payant, tu peux gérer ici la vérification du paiement.
package com.xpertcash.controller.Module;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.Module.ActivationDemande;
import com.xpertcash.DTOs.Module.ModuleDTO;
import com.xpertcash.DTOs.Module.PaiementModuleDTO;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Module.PaiementModule;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.Module.PaiementModuleRepository;
import com.xpertcash.service.Module.ModuleActivationService;

import jakarta.servlet.http.HttpServletRequest;
import com.xpertcash.service.AuthenticationHelper;

@RestController
@RequestMapping("/api/auth")
public class ModuleActivationController {

    @Autowired
    private AuthenticationHelper authHelper;
    @Autowired
    private ModuleActivationService moduleActivationService;
    
    @Autowired
    private JwtUtil jwtUtil; 

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PaiementModuleRepository paiementModuleRepository;
    
    //Endpoint pour consulter le temps restant de la période d'essai pour les modules payants
@GetMapping("/entreprise/temps-essai/modules")
public ResponseEntity<?> consulterTempsEssaiModules(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT manquant ou mal formaté");
    }

    User utilisateur = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = utilisateur.getEntreprise();
    if (entreprise == null) {
        return ResponseEntity.badRequest().body("L'utilisateur n'est associé à aucune entreprise");
    }

    Map<String, String> result = moduleActivationService.consulterTempsRestantEssaiParModule(entreprise);

    return ResponseEntity.ok(result);
}



    //Definir prix des modules seul Super Admin
   @PostMapping("/modules/prix")
    public ResponseEntity<String> changerPrixModule(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        
        String codeModule = (String) body.get("codeModule");
        BigDecimal nouveauPrix = new BigDecimal(body.get("nouveauPrix").toString());

        moduleActivationService.mettreAJourPrixModule(codeModule, nouveauPrix);

        return ResponseEntity.ok("Prix du module mis à jour avec succès.");
    }


    
     //Endpoint pour activer un module
    @PostMapping("/modules/activer")
    public ResponseEntity<?> activerModule(@RequestBody ActivationDemande demande, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        RoleType role = user.getRole().getName();
        if (role != RoleType.ADMIN && role != RoleType.MANAGER) {
            return ResponseEntity.status(403).body("Accès refusé");
        }

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            return ResponseEntity.badRequest().body("Aucune entreprise associée à cet utilisateur");
        }

        try {
            // Appeler le service pour activer le module avec paiement
            moduleActivationService.activerModuleAvecPaiement(
                user.getId(),
                demande.getNomModule(),
                demande.getDureeMois(),
                demande.getNumeroCarte(),
                demande.getCvc(),
                demande.getDateExpiration(),
                demande.getNomCompletProprietaire(),
                demande.getEmailProprietaireCarte(),
                demande.getPays(),
                demande.getAdresse(),
                demande.getVille()
            );

            return ResponseEntity.ok("Module activé avec succès !");
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'activation : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur interne lors de l'activation : " + e.getMessage());
        }
    }

      //Endpoint pour lister tout les  modules actifs ou non actifs
      @GetMapping("/entreprise/modules")
      public ResponseEntity<List<ModuleDTO>> getModulesEntreprise(HttpServletRequest request) {
          List<ModuleDTO> modules = moduleActivationService.listerModulesEntreprise(request);
          return ResponseEntity.ok(modules);
      }
  
    //Endpoint pour vérifier le statut d'activation d'un module spécifique
    @GetMapping("/entreprise/modules/{codeModule}/statut")
    public ResponseEntity<?> verifierStatutModule(
            @PathVariable String codeModule,
            HttpServletRequest request) {
        
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        
        if (entreprise == null) {
            return ResponseEntity.badRequest().body("Aucune entreprise associée à cet utilisateur");
        }

        try {
            boolean moduleActif = moduleActivationService.isModuleActifPourEntreprise(entreprise, codeModule);
            
            Map<String, Object> response = new HashMap<>();
            response.put("codeModule", codeModule);
            response.put("actif", moduleActif);
            response.put("entreprise", entreprise.getNomEntreprise());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la vérification : " + e.getMessage());
        }
    }

    //Endpoint pour renouveler un abonnement expiré
    @PostMapping("/modules/renouveler")
    public ResponseEntity<?> renouvelerAbonnement(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        RoleType role = user.getRole().getName();
        
        if (role != RoleType.ADMIN && role != RoleType.MANAGER) {
            return ResponseEntity.status(403).body("Accès refusé");
        }

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            return ResponseEntity.badRequest().body("Aucune entreprise associée à cet utilisateur");
        }

        try {
            String nomModule = (String) body.get("nomModule");
            int dureeMois = (Integer) body.get("dureeMois");
            String numeroCarte = (String) body.get("numeroCarte");
            String cvc = (String) body.get("cvc");
            String dateExpiration = (String) body.get("dateExpiration");
            String nomCompletProprietaire = (String) body.get("nomCompletProprietaire");
            String emailProprietaireCarte = (String) body.get("emailProprietaireCarte");
            String pays = (String) body.get("pays");
            String adresse = (String) body.get("adresse");
            String ville = (String) body.get("ville");

            // Appeler le service pour renouveler l'abonnement
            moduleActivationService.activerModuleAvecPaiement(
                user.getId(),
                nomModule,
                dureeMois,
                numeroCarte,
                cvc,
                dateExpiration,
                nomCompletProprietaire,
                emailProprietaireCarte,
                pays,
                adresse,
                ville
            );

            return ResponseEntity.ok("Abonnement renouvelé avec succès !");
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur lors du renouvellement : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur interne lors du renouvellement : " + e.getMessage());
        }
    }

    //Endpoint pour consulter les abonnements actifs d'une entreprise
    @GetMapping("/entreprise/abonnements")
    public ResponseEntity<?> consulterAbonnementsActifs(HttpServletRequest request) {
        
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        
        if (entreprise == null) {
            return ResponseEntity.badRequest().body("Aucune entreprise associée à cet utilisateur");
        }

        try {
            List<Map<String, Object>> abonnements = moduleActivationService.consulterAbonnementsActifs(entreprise);
            return ResponseEntity.ok(abonnements);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors de la consultation : " + e.getMessage());
        }
    }

    //Endpoint pour désactiver manuellement un module
    @PostMapping("/modules/desactiver")
    public ResponseEntity<?> desactiverModule(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        RoleType role = user.getRole().getName();
        
        if (role != RoleType.ADMIN) {
            return ResponseEntity.status(403).body("Seuls les ADMIN peuvent désactiver des modules");
        }

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            return ResponseEntity.badRequest().body("Aucune entreprise associée à cet utilisateur");
        }

        try {
            String codeModule = body.get("codeModule");
            
            if (codeModule == null || codeModule.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le code du module est requis");
            }

            boolean desactive = moduleActivationService.desactiverModulePourEntreprise(entreprise, codeModule);
            
            if (desactive) {
                return ResponseEntity.ok("Module désactivé avec succès");
            } else {
                return ResponseEntity.badRequest().body("Module non trouvé ou déjà inactif");
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur lors de la désactivation : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur interne lors de la désactivation : " + e.getMessage());
        }
    }
  

    //Total des paiements par module.

    @GetMapping("/admin/paiements-par-module")
    public ResponseEntity<?> getPaiementsParModule(HttpServletRequest request) {

        /*String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token manquant");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getRole().getName() != RoleType.SUPER_ADMIN) {
            return ResponseEntity.status(403).body("Accès réservé au propriétaire de la plateforme");
        }*/

        List<PaiementModule> paiements = paiementModuleRepository.findAll();

        Map<String, Map<String, Object>> regroupement = new HashMap<>();

        for (PaiementModule paiement : paiements) {

            String code = paiement.getModule().getCode();
            String nom = paiement.getModule().getNom();

            String cle = code;

            if (!regroupement.containsKey(cle)) {
                Map<String, Object> moduleData = new HashMap<>();
                moduleData.put("codeModule", code);
                moduleData.put("nomModule", nom);
                moduleData.put("totalPaiements", 0);
                moduleData.put("montantTotal", BigDecimal.ZERO);
                moduleData.put("paiements", new ArrayList<Map<String, Object>>());
                regroupement.put(cle, moduleData);
            }

            Map<String, Object> moduleData = regroupement.get(cle);

            // Incrément du compteur et du montant
            moduleData.put("totalPaiements", (Integer) moduleData.get("totalPaiements") + 1);
            moduleData.put("montantTotal", ((BigDecimal) moduleData.get("montantTotal")).add(paiement.getMontant()));

            // Ajouter le détail du paiement
            Map<String, Object> paiementData = new HashMap<>();
            paiementData.put("montant", paiement.getMontant());
            paiementData.put("devise", paiement.getDevise());
            paiementData.put("nomCompletProprietaire", paiement.getNomCompletProprietaire());
            paiementData.put("emailProprietaireCarte", paiement.getEmailProprietaireCarte());
            paiementData.put("datePaiement", paiement.getDatePaiement());
            paiementData.put("entrepriseNom", paiement.getEntreprise().getNomEntreprise());
            paiementData.put("referenceTransaction", paiement.getReferenceTransaction());

            ((List<Map<String, Object>>) moduleData.get("paiements")).add(paiementData);
        }

        return ResponseEntity.ok(regroupement.values());
    }




}

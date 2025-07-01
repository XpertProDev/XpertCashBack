package com.xpertcash.controller.Module;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

@RestController
@RequestMapping("/api/auth")
public class ModuleActivationController {
    @Autowired
    private ModuleActivationService moduleActivationService;
    
    @Autowired
    private JwtUtil jwtUtil; 

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PaiementModuleRepository paiementModuleRepository;
    

    //Definir prix des modules
    
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

        Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

        moduleActivationService.activerModuleAvecPaiement(
                userId,
                demande.getNomModule(),
                demande.getNumeroCarte(),
                demande.getCvc(),
                demande.getDateExpiration(),
                demande.getNomProprietaire(),
                demande.getPrenomProprietaire(),
                demande.getEmailProprietaireCarte(),
                demande.getAdresse(),
                demande.getVille()
        );

        return ResponseEntity.ok("Le module '" + demande.getNomModule() + "' a été activé avec succès.");
    }



    //Endpoint pour lister tout les  modules actifs ou non actifs
    @GetMapping("/entreprise/modules")
    public ResponseEntity<List<ModuleDTO>> getModulesEntreprise(HttpServletRequest request) {
        List<ModuleDTO> modules = moduleActivationService.listerModulesEntreprise(request);
        return ResponseEntity.ok(modules);
    }


    //Les Facture de payement pour lentreprise
    @GetMapping("/mes-paiements")
    public ResponseEntity<?> getPaiementsModules(HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token manquant");
        }

        Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        RoleType role = user.getRole().getName();
        if (role != RoleType.ADMIN && role != RoleType.MANAGER) {
            return ResponseEntity.status(403).body("Accès refusé");
        }

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            return ResponseEntity.badRequest().body("Aucune entreprise associée à cet utilisateur");
        }

        List<PaiementModule> paiements = paiementModuleRepository.findByEntrepriseId(entreprise.getId());

        List<PaiementModuleDTO> paiementDTOS = paiements.stream()
                .map(PaiementModuleDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(paiementDTOS);
    }


    //Total des paiements par module.

    @GetMapping("/admin/paiements-par-module")
    public ResponseEntity<?> getPaiementsParModule(HttpServletRequest request) {

        /*String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token manquant");
        }

        Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

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
            paiementData.put("nomProprietaire", paiement.getNomProprietaire());
            paiementData.put("prenomProprietaire", paiement.getPrenomProprietaire());
            paiementData.put("emailProprietaireCarte", paiement.getEmailProprietaireCarte());
            paiementData.put("datePaiement", paiement.getDatePaiement());
            paiementData.put("entrepriseNom", paiement.getEntreprise().getNomEntreprise());
            paiementData.put("referenceTransaction", paiement.getReferenceTransaction());

            ((List<Map<String, Object>>) moduleData.get("paiements")).add(paiementData);
        }

        return ResponseEntity.ok(regroupement.values());
    }


}

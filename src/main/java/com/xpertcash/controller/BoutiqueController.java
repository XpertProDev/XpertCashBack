package com.xpertcash.controller;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.BoutiqueResponse;
import com.xpertcash.DTOs.TransfertDTO;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Transfert;
import com.xpertcash.service.BoutiqueService;
import com.xpertcash.repository.TransfertRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class BoutiqueController {

  
   
    @Autowired
    private BoutiqueService boutiqueService;
    @Autowired
    private TransfertRepository transfertRepository;

    // Ajouter une boutique (requête JSON)
    @PostMapping("/ajouterBoutique")
    public ResponseEntity<Map<String, String>> ajouterBoutique(
            HttpServletRequest request,
            @RequestBody Map<String, String> boutiqueDetails) {
        
        Map<String, String> response = new HashMap<>();

        try {
            // Extraire les données depuis la requête
            String nomBoutique = boutiqueDetails.get("nomBoutique");
            String adresse = boutiqueDetails.get("adresse");
            String telephone = boutiqueDetails.get("telephone");
            String email = boutiqueDetails.get("email");

            Boutique nouvelleBoutique = boutiqueService.ajouterBoutique(request, nomBoutique, adresse, telephone, email);
            
            response.put("message", "Boutique ajoutée avec succès !");
            return ResponseEntity.ok(response);
        
        } catch (RuntimeException e) {
            System.err.println("🔴 ERREUR : " + e.getMessage());

            if (e.getMessage().contains("Token JWT")) {
                response.put("error", "Votre session a expiré. Veuillez vous reconnecter.");
            } 
            else if (e.getMessage().contains("Seul un admin peut ajouter une boutique")) {
                response.put("error", "Accès refusé : Vous n'avez pas les droits nécessaires.");
            } 
            else if (e.getMessage().contains("L'Admin n'a pas d'entreprise associée")) {
                response.put("error", "Vous devez d'abord créer une entreprise avant d'ajouter une boutique.");
            } 
            else {
                response.put("error", "Impossible d'ajouter la boutique. Veuillez réessayer plus tard.");
            }

            return ResponseEntity.badRequest().body(response);
        }
    }

    // Récupérer toutes les boutiques d'une entreprise
    @GetMapping("/boutiqueEntreprise")
    public ResponseEntity<List<BoutiqueResponse>> getBoutiquesByEntreprise(HttpServletRequest request) {
        List<Boutique> boutiques = boutiqueService.getBoutiquesByEntreprise(request);

        List<BoutiqueResponse> boutiqueResponses = boutiques.stream()
            .map(boutique -> new BoutiqueResponse(
                boutique.getId(),
                boutique.getNomBoutique(),
                boutique.getAdresse(),
                boutique.getTelephone(),
                boutique.getEmail(),
                boutique.getCreatedAt(),
                boutique.isActif()
                
            ))
            .toList();

        return ResponseEntity.ok(boutiqueResponses);
    }




    //Endpoint Update Boutique
    @PutMapping("/updatedBoutique/{id}")
    public ResponseEntity<Map<String, String>> updateBoutique(
        @PathVariable Long id,
        @RequestBody Map<String, String> updates,
        HttpServletRequest request) {
    
    Map<String, String> response = new HashMap<>();

    try {
        // Extraire les nouveaux noms depuis le JSON
        String newNomBoutique = updates.get("nomBoutique");
        String newAdresse = updates.get("adresse");
        String newTelephone = updates.get("telephone");
        String newEmail = updates.get("email");


        Boutique updatedBoutique = boutiqueService.updateBoutique(id, newNomBoutique, newAdresse, newTelephone, newEmail, request);
        
        response.put("message", "Boutique mise à jour avec succès !");
        return ResponseEntity.ok(response);
    
    } catch (RuntimeException e) {

        // Cas ou le problème vient du token
        if (e.getMessage().contains("Token JWT")) {
            response.put("error", "Votre session a expiré. Veuillez vous reconnecter.");
        } 
        // Cas ou l'utilisateur n'est pas admin
        else if (e.getMessage().contains("Seul un ADMIN peut modifier une boutique")) {
            response.put("error", "Accès refusé : Vous n'avez pas les droits nécessaires.");
        } 
        else {
            response.put("error", "Impossible de modifier la boutique. Vérifiez vos permissions ou réessayez plus tard.");
        }
        
        return ResponseEntity.badRequest().body(response);
    }
}

    //Endpoint listing Produit boutique

    @PostMapping("/transferer-produits")
    public ResponseEntity<String> transfererProduits(
            HttpServletRequest request,
            @RequestBody Map<String, Object> transfertDetails) {
        Long boutiqueSourceId = Long.valueOf(transfertDetails.get("boutiqueSourceId").toString());
        Long boutiqueDestinationId = Long.valueOf(transfertDetails.get("boutiqueDestinationId").toString());
        Long produitId = Long.valueOf(transfertDetails.get("produitId").toString());
        int quantite = Integer.parseInt(transfertDetails.get("quantite").toString());

        boutiqueService.transfererProduits(request, boutiqueSourceId, boutiqueDestinationId, produitId, quantite);
        return ResponseEntity.ok("Transfert de produits effectué avec succès.");
    }

    @GetMapping("/boutique/{id}/produits")
    public ResponseEntity<List<Produit>> getProduitsParBoutique(
            HttpServletRequest request,
            @PathVariable Long id) {
        List<Produit> produits = boutiqueService.getProduitsParBoutique(request, id);
        return ResponseEntity.ok(produits);
    }

    @GetMapping("/transferts")
    public ResponseEntity<Object> getTransferts(
            @RequestParam(required = false) Long boutiqueId) {
        List<Transfert> transferts;
        if (boutiqueId != null) {
            transferts = transfertRepository.findByBoutiqueSourceIdOrBoutiqueDestinationId(boutiqueId, boutiqueId);
        } else {
            transferts = transfertRepository.findAll();
        }

        if (transferts.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Aucun transfert trouvé.");
            return ResponseEntity.ok(response);
        }

        // Convertir les entités Transfert en DTO
        List<TransfertDTO> transfertDTOs = transferts.stream().map(transfert -> {
            TransfertDTO dto = new TransfertDTO();
            dto.setId(transfert.getId());
            dto.setProduitNom(transfert.getProduit().getNom());
            dto.setProduitCodeGenerique(transfert.getProduit().getCodeGenerique());
            dto.setBoutiqueSourceNom(transfert.getBoutiqueSource().getNomBoutique());
            dto.setBoutiqueDestinationNom(transfert.getBoutiqueDestination().getNomBoutique());
            dto.setQuantite(transfert.getQuantite());
            dto.setDateTransfert(transfert.getDateTransfert().toString());
            return dto; 
        }).toList();

        return ResponseEntity.ok(transfertDTOs);
    }

    // Endpoint pour Desactiver une boutique
    @PutMapping("/desactiverBoutique/{id}")
    public ResponseEntity<Map<String, String>> desactiverBoutique(
            @PathVariable Long id,
            HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            boutiqueService.desactiverBoutique(id, request);
            response.put("message", "Boutique désactivée avec succès !");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    // Endpoint pour réactiver une boutique
    @PutMapping("/activerBoutique/{id}")
    public ResponseEntity<Map<String, String>> reactiverBoutique(
            @PathVariable Long id,
            HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();

        try {
            boutiqueService.activerBoutique(id, request);
            response.put("message", "Boutique réactivée avec succès !");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}

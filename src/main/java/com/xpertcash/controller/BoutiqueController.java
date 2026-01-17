package com.xpertcash.controller;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.TransfertDTO;
import com.xpertcash.DTOs.Boutique.BoutiqueResponse;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Transfert;
import com.xpertcash.entity.Enum.TypeBoutique;
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
            String nomBoutique = boutiqueDetails.get("nomBoutique");
            String adresse = boutiqueDetails.get("adresse");
            String telephone = boutiqueDetails.get("telephone");
            String email = boutiqueDetails.get("email");
            String typeString = boutiqueDetails.get("type");

            if (typeString == null || typeString.isEmpty()) {
                throw new RuntimeException("Le type de boutique est requis (BOUTIQUE ou ENTREPOT).");
            }

            TypeBoutique typeBoutique;
            try {
                typeBoutique = TypeBoutique.valueOf(typeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Type de boutique invalide. Utilisez BOUTIQUE ou ENTREPOT.");
            }

            Boutique nouvelleBoutique = boutiqueService.ajouterBoutique(request, nomBoutique, adresse, telephone, email, typeBoutique);

            response.put("message", "Boutique ajoutée avec succès !");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.err.println(" ERREUR : " + e.getMessage());

            if (e.getMessage().contains("Token JWT")) {
                response.put("error", "Votre session a expiré. Veuillez vous reconnecter.");
            } 
            else if (e.getMessage().contains("Seul un admin peut ajouter une boutique")) {
                response.put("error", "Accès refusé : Vous n'avez pas les droits nécessaires.");
            } 
            else if (e.getMessage().contains("L'Admin n'a pas d'entreprise associée")) {
                response.put("error", "Vous devez d'abord créer une entreprise avant d'ajouter une boutique.");
            }
            else if (e.getMessage().contains("Type de boutique invalide") || e.getMessage().contains("Le type de boutique est requis")) {
                response.put("error", e.getMessage());
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
                boutique.isActif(),
                boutique.getTypeBoutique(),
                null,
                null
                
            ))
            .toList();

        return ResponseEntity.ok(boutiqueResponses);
    }

   // Endpoint recuperer une boutique par son ID
    @GetMapping("/boutique/{id}")
    public ResponseEntity<BoutiqueResponse> getBoutiqueById(@PathVariable Long id, HttpServletRequest request) {
        try {
            Boutique boutique = boutiqueService.getBoutiqueById(id, request);
            BoutiqueResponse response = new BoutiqueResponse(
                boutique.getId(),
                boutique.getNomBoutique(),
                boutique.getAdresse(),
                boutique.getTelephone(),
                boutique.getEmail(),
                boutique.getCreatedAt(),
                boutique.isActif(),
                boutique.getTypeBoutique(),
                null,
                null


            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
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

        if (e.getMessage().contains("Token JWT")) {
            response.put("error", "Votre session a expiré. Veuillez vous reconnecter.");
        } 
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

    //Endpoint pour copier
    @PostMapping("/copier-produits")
    public ResponseEntity<Map<String, Object>> copierProduits(
            HttpServletRequest request,
            @RequestBody Map<String, Object> detailsCopie) {

        Long boutiqueSourceId = Long.valueOf(detailsCopie.get("boutiqueSourceId").toString());
        Long boutiqueDestinationId = Long.valueOf(detailsCopie.get("boutiqueDestinationId").toString());
        boolean toutCopier = Boolean.parseBoolean(detailsCopie.get("toutCopier").toString());

        List<Long> listeProduitIds = new ArrayList<>();
        if (!toutCopier && detailsCopie.containsKey("produitIds")) {
            List<Object> ids = (List<Object>) detailsCopie.get("produitIds");
            for (Object id : ids) {
                listeProduitIds.add(Long.valueOf(id.toString()));
            }
        }

        try {
            // Appel unique au service
            int produitsCopies = boutiqueService.copierProduits(
                    request,
                    boutiqueSourceId,
                    boutiqueDestinationId,
                    listeProduitIds,
                    toutCopier
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", produitsCopies + " produit(s) copié(s) avec succès.");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }


    @GetMapping("/boutique/{id}/produits")
 public ResponseEntity<List<ProduitDTO>> getProduitsParBoutique(
        HttpServletRequest request,
        @PathVariable Long id) {
    List<ProduitDTO> produitsDTO = boutiqueService.getProduitsParBoutique(request, id);
    return ResponseEntity.ok(produitsDTO);
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

    //Endpoint pour lister vendeur dune boutique spécifique

    //     @GetMapping("/{boutiqueId}/vendeurs")
    // public ResponseEntity<List<User>> getVendeursByBoutique(
    //         @PathVariable Long boutiqueId,
    //         HttpServletRequest request
    // ) {
    //     List<User> vendeurs = boutiqueService.getVendeursByBoutique(boutiqueId, request);
    //     return ResponseEntity.ok(vendeurs);
    // }


    // Suprimer une boutique

   @DeleteMapping("/boutique/{id}")
        public ResponseEntity<?> supprimerBoutique(@PathVariable Long id,
        HttpServletRequest request) {
            return boutiqueService.supprimerBoutique(id, request);
        }









}

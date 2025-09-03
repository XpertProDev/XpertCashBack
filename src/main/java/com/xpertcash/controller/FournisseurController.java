package com.xpertcash.controller;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpertcash.DTOs.FOURNISSEUR.FournisseurDTO;
import com.xpertcash.DTOs.FOURNISSEUR.FournisseurResponseDTO;
import com.xpertcash.service.AuthenticationHelper;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.entity.User;
import com.xpertcash.repository.FactureRepository;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.repository.StockProduitFournisseurRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.FournisseurService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/api/auth")
public class FournisseurController {

    @Autowired
    private FournisseurService fournisseurService;
    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private StockProduitFournisseurRepository stockProduitFournisseurRepository;


    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private AuthenticationHelper authHelper;
    @Autowired
    private UsersRepository usersRepository;

    // Create fournisseur
  @PostMapping(value = "/save-fournisseurs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveFournisseur(
            @RequestPart("fournisseur") String fournisseurJson,
            @RequestPart(value = "imageFournisseurFile", required = false) MultipartFile imageFournisseurFile,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Convertir JSON vers DTO d'entrée
            ObjectMapper objectMapper = new ObjectMapper();
            FournisseurDTO fournisseurDTO = objectMapper.readValue(fournisseurJson, FournisseurDTO.class);

            // Mapping DTO vers entité
            Fournisseur fournisseur = new Fournisseur();
            fournisseur.setNomComplet(fournisseurDTO.getNomComplet());
            fournisseur.setNomSociete(fournisseurDTO.getNomSociete());
            fournisseur.setEmail(fournisseurDTO.getEmail());
            fournisseur.setTelephone(fournisseurDTO.getTelephone());
            fournisseur.setAdresse(fournisseurDTO.getAdresse());
            fournisseur.setPays(fournisseurDTO.getPays());
            fournisseur.setVille(fournisseurDTO.getVille());
            fournisseur.setDescription(fournisseurDTO.getDescription());

            // Sauvegarde
            Fournisseur savedFournisseur = fournisseurService.saveFournisseur(fournisseur, imageFournisseurFile, request);

            // Construction du DTO de réponse
            FournisseurResponseDTO responseDTO = new FournisseurResponseDTO();
            responseDTO.setId(savedFournisseur.getId());
            responseDTO.setNomComplet(savedFournisseur.getNomComplet());
            responseDTO.setNomSociete(savedFournisseur.getNomSociete());
            responseDTO.setEmail(savedFournisseur.getEmail());
            responseDTO.setTelephone(savedFournisseur.getTelephone());
            responseDTO.setAdresse(savedFournisseur.getAdresse());
            responseDTO.setPays(savedFournisseur.getPays());
            responseDTO.setVille(savedFournisseur.getVille());
            responseDTO.setDescription(savedFournisseur.getDescription());
            responseDTO.setPhoto(savedFournisseur.getPhoto());
            if (savedFournisseur.getEntreprise() != null) {
                responseDTO.setEntrepriseId(savedFournisseur.getEntreprise().getId());
                responseDTO.setEntrepriseNom(savedFournisseur.getEntreprise().getNomEntreprise());
            }

            // Réponse propre
            response.put("message", "Fournisseur enregistré avec succès !");
            response.put("fournisseur", responseDTO);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors du traitement : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

     // Get All fournisseurs dune entreprise de l utilisateur 
    @GetMapping("/get-fournisseurs")
    public ResponseEntity<List<Map<String, Object>>> getFournisseursByEntreprise(HttpServletRequest request) {
        List<Fournisseur> fournisseurs = fournisseurService.getFournisseursByEntreprise(request);

        List<Map<String, Object>> result = fournisseurs.stream().map(fournisseur -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", fournisseur.getId());
            map.put("nomComplet", fournisseur.getNomComplet());
            map.put("nomSociete", fournisseur.getNomSociete());
            map.put("description", fournisseur.getDescription());
            map.put("adresse", fournisseur.getAdresse());
            map.put("pays", fournisseur.getPays());
            map.put("ville", fournisseur.getVille());
            map.put("telephone", fournisseur.getTelephone());
            map.put("email", fournisseur.getEmail());
            map.put("createdAt", fournisseur.getCreatedAt());
            map.put("photo", fournisseur.getPhoto());

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

      
    //Get fournisseur by id
    @GetMapping("/getFournisseur/{id}")
public ResponseEntity<?> getFournisseurById(@PathVariable Long id, HttpServletRequest request) {

    try {
        Fournisseur fournisseur = fournisseurService.getFournisseurById(id, request);

        FournisseurResponseDTO responseDTO = new FournisseurResponseDTO();
        responseDTO.setId(fournisseur.getId());
        responseDTO.setNomComplet(fournisseur.getNomComplet());
        responseDTO.setNomSociete(fournisseur.getNomSociete());
        responseDTO.setEmail(fournisseur.getEmail());
        responseDTO.setTelephone(fournisseur.getTelephone());
        responseDTO.setAdresse(fournisseur.getAdresse());
        responseDTO.setPays(fournisseur.getPays());
        responseDTO.setVille(fournisseur.getVille());
        responseDTO.setDescription(fournisseur.getDescription());
        responseDTO.setPhoto(fournisseur.getPhoto());

        if (fournisseur.getEntreprise() != null) {
            responseDTO.setEntrepriseId(fournisseur.getEntreprise().getId());
            responseDTO.setEntrepriseNom(fournisseur.getEntreprise().getNomEntreprise());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("fournisseur", responseDTO);

        return ResponseEntity.ok(response);

    } catch (RuntimeException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}


    //Update fournisseur
    @PutMapping(value = "/updateFournisseur/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateFournisseur(
        @PathVariable Long id,
        @RequestPart("updatedFournisseur") Fournisseur updatedFournisseur,
        @RequestPart(value = "imageFournisseurFile", required = false) MultipartFile imageFournisseurFile,
        HttpServletRequest request) {

    Map<String, Object> response = new HashMap<>();

    try {
        Fournisseur updated = fournisseurService.updateFournisseur(id, updatedFournisseur, imageFournisseurFile, request);

        response.put("message", "Fournisseur mis à jour avec succès");
        response.put("fournisseur", updated);
        return ResponseEntity.ok(response);

    } catch (RuntimeException e) {
        response.put("message", "Erreur : " + e.getMessage());
        e.printStackTrace(); // Optionnel pour debug
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

    } catch (Exception e) {
        response.put("message", "Une erreur interne est survenue.");
        e.printStackTrace(); // Pour afficher la vraie erreur serveur dans les logs
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}


     @GetMapping("/quantite-par-fournisseur/{produitId}")
    public ResponseEntity<?> getQuantiteParFournisseur(@PathVariable Long produitId) {
        List<Object[]> resultats = stockProduitFournisseurRepository.findQuantiteParFournisseurPourProduit(produitId);
        List<Map<String, Object>> data = resultats.stream().map(obj -> {
            Map<String, Object> map = new HashMap<>();
            map.put("fournisseur", obj[0]);
            map.put("quantite", obj[1]);
            map.put("produitId", produitId);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    //Get quantite d'un produit par fournisseur
    @GetMapping("/quantite-ajoutee-par-fournisseur/{fournisseurId}")
    public ResponseEntity<List<Map<String, Object>>> getQuantiteAjoutee(
            @PathVariable Long fournisseurId,
            HttpServletRequest request
    ) {
        List<Map<String, Object>> result = fournisseurService.getNomProduitEtQuantiteAjoutee(fournisseurId, request);
        return ResponseEntity.ok(result);
    }


    //Get fournisseur lier a des factures
    @GetMapping("/factures-par-fournisseur/{fournisseurId}")
    public ResponseEntity<List<Map<String, Object>>> getFacturesParFournisseur(@PathVariable Long fournisseurId,
         HttpServletRequest request) {

        User user = authHelper.getAuthenticatedUserWithFallback(request);

    // 2. Vérifier que le fournisseur appartient à la même entreprise que l'utilisateur
    Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable !"));

    if (!fournisseur.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès refusé : Ce fournisseur n'appartient pas à votre entreprise !");
    }

         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Facture> factures = factureRepository.findByFournisseur_Id(fournisseurId);

        List<Map<String, Object>> factureDTOs = factures.stream().map(facture -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", facture.getId());
            map.put("type", facture.getType());
            map.put("description", facture.getDescription());
            map.put("dateFacture", facture.getDateFacture().format(formatter));
            map.put("numeroFacture", facture.getNumeroFacture());
            map.put("codeFournisseur", facture.getCodeFournisseur());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(factureDTOs);
    }



    // Delete fournisseur
    @DeleteMapping("/fournisseur/{id}")
    public ResponseEntity<Map<String, String>> supprimerFournisseur(
        @PathVariable Long id,
        HttpServletRequest request) {

        try {
            fournisseurService.supprimerFournisseur(id, request);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Fournisseur supprimé avec succès.");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            Map<String, String> response = new HashMap<>();
            response.put("message", ex.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

}

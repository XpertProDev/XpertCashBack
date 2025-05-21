package com.xpertcash.controller;

import java.time.format.DateTimeFormatter;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.FournisseurDTO;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.entity.User;
import com.xpertcash.repository.FactureRepository;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.repository.StockProduitFournisseurRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.FournisseurService;
import jakarta.servlet.http.HttpServletRequest;

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
    private JwtUtil jwtUtil;
    @Autowired
    private UsersRepository usersRepository;

    @PostMapping("/save-fournisseurs")
    public ResponseEntity<?> saveFournisseur(
            @RequestBody FournisseurDTO fournisseurDTO,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Mapping DTO vers entité
            Fournisseur fournisseur = new Fournisseur();
            fournisseur.setNomComplet(fournisseurDTO.getNomComplet());
            fournisseur.setEmail(fournisseurDTO.getEmail());
            fournisseur.setTelephone(fournisseurDTO.getTelephone());
            fournisseur.setAdresse(fournisseurDTO.getAdresse());
            fournisseur.setPays(fournisseurDTO.getPays());
            fournisseur.setVille(fournisseurDTO.getVille());


            // Appel au service
            Fournisseur savedFournisseur = fournisseurService.saveFournisseur(fournisseur, request);
            response.put("message", "Fournisseur enregistré avec succès !");
            response.put("fournisseur", savedFournisseur);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
            map.put("adresse", fournisseur.getAdresse());
            map.put("pays", fournisseur.getPays());
            map.put("ville", fournisseur.getVille());
            map.put("telephone", fournisseur.getTelephone());
            map.put("email", fournisseur.getEmail());
            map.put("createdAt", fournisseur.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

      
    //Get fournisseur by id
    @GetMapping("/getFournisseur/{id}")
         public ResponseEntity<Fournisseur> getFournisseurById(@PathVariable Long id, HttpServletRequest request) {
         Fournisseur fournisseur = fournisseurService.getFournisseurById(id, request);
                return ResponseEntity.ok(fournisseur);
    }

    //Update fournisseur
      @PutMapping("/updateFournisseur/{id}")
        public ResponseEntity<Map<String, Object>> updateFournisseur(
        @PathVariable Long id,
        @RequestBody Fournisseur updatedFournisseur,
        HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            Fournisseur updated = fournisseurService.updateFournisseur(id, updatedFournisseur, request);

            response.put("message", "Fournisseur mis à jour avec succès");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("message", "Erreur : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            response.put("message", "Une erreur interne est survenue.");
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

        // 1. Extraire l'utilisateur depuis le token
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
    }

    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

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






}

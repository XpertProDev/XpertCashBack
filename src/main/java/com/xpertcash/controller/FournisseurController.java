package com.xpertcash.controller;

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
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.repository.StockProduitFournisseurRepository;
import com.xpertcash.service.FournisseurService;
import com.xpertcash.service.ProduitService;

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
    private ProduitService produitService;

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

     // Get fournisseur dune entreprise de l utilisateur 
     @GetMapping("/get-fournisseurs")
     public ResponseEntity<List<Fournisseur>> getFournisseursByEntreprise(HttpServletRequest request) {
        List<Fournisseur> fournisseurs = fournisseurService.getFournisseursByEntreprise(request);
        return ResponseEntity.ok(fournisseurs);
    }
      
    //Get fournisseur by id
    @GetMapping("/getFournisseur/{id}")
         public ResponseEntity<Fournisseur> getFournisseurById(@PathVariable Long id, HttpServletRequest request) {
         Fournisseur fournisseur = fournisseurService.getFournisseurById(id, request);
                return ResponseEntity.ok(fournisseur);
    }

    //Update fournisseur

    @PutMapping("/updateFournisseur/{id}")
    public ResponseEntity<Fournisseur> updateFournisseur(
            @PathVariable Long id,
            @RequestBody Fournisseur updatedFournisseur,
            HttpServletRequest request) {

        Fournisseur updated = fournisseurService.updateFournisseur(id, updatedFournisseur, request);
        return ResponseEntity.ok(updated);
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
    @GetMapping("/stock-par-fournisseur/{fournisseurId}")
    public ResponseEntity<List<Map<String, Object>>> getStockParFournisseurSimplifie(@PathVariable Long fournisseurId) {
        return ResponseEntity.ok(fournisseurService.getNomProduitEtQuantiteAjoutee(fournisseurId));
    }


}

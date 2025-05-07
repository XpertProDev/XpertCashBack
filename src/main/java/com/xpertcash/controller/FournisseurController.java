package com.xpertcash.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.FournisseurDTO;
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.service.FournisseurService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class FournisseurController {

    @Autowired
    private FournisseurService fournisseurService;
    @Autowired
    private FournisseurRepository fournisseurRepository;

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
     public ResponseEntity<List<Fournisseur>> getFournisseurs() {
         List<Fournisseur> fournisseurs = fournisseurRepository.findAll();
         return ResponseEntity.ok(fournisseurs);
     }
     


}

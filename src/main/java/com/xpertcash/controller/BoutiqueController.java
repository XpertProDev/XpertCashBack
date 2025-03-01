package com.xpertcash.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Produit;
import com.xpertcash.service.BoutiqueService;
import com.xpertcash.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class BoutiqueController {

    @Autowired
    private UsersService usersService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthorizationService authorizationService;
     @Autowired
    private BoutiqueService boutiqueService;


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

            Boutique nouvelleBoutique = boutiqueService.ajouterBoutique(request, nomBoutique, adresse);
            
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
    public ResponseEntity<List<Boutique>> getBoutiquesByEntreprise(HttpServletRequest request) {
        // Utilisation du service pour récupérer les boutiques
        List<Boutique> boutiques = boutiqueService.getBoutiquesByEntreprise(request);

        // Retourner la liste des boutiques
        return ResponseEntity.ok(boutiques);
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

        Boutique updatedBoutique = boutiqueService.updateBoutique(id, newNomBoutique, newAdresse, request);
        
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
}

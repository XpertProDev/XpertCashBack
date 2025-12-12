package com.xpertcash.controller;

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

import com.xpertcash.entity.Unite;
import com.xpertcash.service.UniteService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class UniteController {

     @Autowired
    private UniteService uniteService;

     // Créer une unité de mesure

    @PostMapping("/createUnite")
    public ResponseEntity<Object> createUnite(@RequestBody Map<String, String> payload, HttpServletRequest request) {
    try {
        String nom = payload.get("nom");

        if (nom == null || nom.isEmpty()) {
            throw new RuntimeException("L'unité ne doit pas être vide !");
        }

        Unite unite = new Unite();
        unite.setNom(nom);

        // Utiliser le service qui va associer automatiquement l'entreprise de l'utilisateur
        Unite createdUnite = uniteService.createUnite(unite, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdUnite);
    } catch (IllegalArgumentException e) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        // Vérifier si c'est une erreur de duplication
        if (e.getMessage().contains("existe déjà")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    } catch (RuntimeException e) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}

    //Update Unite
    @PutMapping("/updateUnite/{uniteId}")
    public ResponseEntity<Unite> updateUnite(@PathVariable Long uniteId, @RequestBody Unite uniteDetails, HttpServletRequest request) {
        try {
            
            // Appeler le service pour mettre à jour l'unité
            Unite updatedUnite = uniteService.updateUnite(request, uniteId, uniteDetails);
            return ResponseEntity.ok(updatedUnite);  // Retourner l'unité mise à jour
        } catch (RuntimeException e) {
            // Retour d'une erreur en cas de problème
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

     // Récupérer toutes les unités de l'entreprise de l'utilisateur connecté
     @GetMapping("/allUnite")
     public ResponseEntity<List<Unite>> getAllUnites(HttpServletRequest request) {
         try {
             List<Unite> unites = uniteService.getAllUnites(request);
             return ResponseEntity.ok(unites);
         } catch (RuntimeException e) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }
     }
    

    
     //Récupérer unité par son ID (vérifie qu'elle appartient à l'entreprise de l'utilisateur)
    @GetMapping("/unite/{id}")
    public ResponseEntity<Unite> getUniteById(@PathVariable Long id, HttpServletRequest request) {
        try {
            Unite unite = uniteService.getUniteById(id, request);
            return ResponseEntity.status(HttpStatus.OK).body(unite);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}

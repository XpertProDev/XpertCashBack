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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.entity.Unite;
import com.xpertcash.repository.UniteRepository;
import com.xpertcash.service.UniteService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class UniteController {

     @Autowired
    private UniteService uniteService;
    @Autowired
    private UniteRepository uniteRepository;

     // Créer une unité de mesure
   @PostMapping("/createUnite")
public ResponseEntity<Object> createUnite(@RequestBody Map<String, String> payload) {
    try {
        String nom = payload.get("nom");

        if (nom == null || nom.isEmpty()) {
            throw new RuntimeException("Le nom de l'unité ne peut pas être vide !");
        }

        if (uniteRepository.existsByNom(nom)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Cette unité de mesure existe déjà !");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        Unite unite = new Unite();
        unite.setNom(nom);

        Unite createdUnite = uniteRepository.save(unite);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdUnite);
    } catch (RuntimeException e) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}

//Update Unite
@PutMapping("/updateUnite/{uniteId}")
public ResponseEntity<Object> updateUnite(@PathVariable Long uniteId, @RequestBody Unite uniteDetails, HttpServletRequest request) {
    try {
        // Appel du service pour mettre à jour l'unité
        Unite updatedUnite = uniteService.updateUnite(request, uniteId, uniteDetails);
        return ResponseEntity.ok(updatedUnite);  // Retourner l'unité mise à jour en cas de succès
    } catch (RuntimeException e) {
        // Capture de l'exception avec un message détaillé
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        
        // Retourner une erreur avec message détaillé
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse); 
    }
}


    

    
     // Récupérer toutes les unités 
     @GetMapping("/allUnite")
     public ResponseEntity<List<Unite>> getAllUnites() {
         try {
             List<Unite> unites = uniteRepository.findAll();
             return ResponseEntity.ok(unites);
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }
     }
     

    
     //Récupérer unité par son ID
    @GetMapping("/unite/{id}")
    public ResponseEntity<Unite> getUniteById(@PathVariable Long id) {
        try {
            Unite unite = uniteService.getUniteById(id);
            return ResponseEntity.status(HttpStatus.OK).body(unite);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}

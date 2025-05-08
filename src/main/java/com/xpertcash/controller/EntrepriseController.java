package com.xpertcash.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import com.xpertcash.service.IMAGES.ImageStorageService;



import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.DTOs.UpdateEntrepriseDTO;
import com.xpertcash.service.EntrepriseService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class EntrepriseController {

     @Autowired
    private EntrepriseService entrepriseService;
  

    @GetMapping("/allentreprises")
    public ResponseEntity<?> getAllEntreprises() {
        try {
            List<EntrepriseDTO> entreprises = entrepriseService.getAllEntreprisesWithInfo();
            return ResponseEntity.ok(entreprises);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

    @PostMapping("/debug-dto")
    public ResponseEntity<?> debugDto(@RequestBody UpdateEntrepriseDTO dto) {
        System.out.println("➡️ DTO reçu : " + dto);
        return ResponseEntity.ok(dto);
    }

  
    
    @PatchMapping(value = "/updateEntreprise/{entrepriseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateEntreprise(
            @PathVariable Long entrepriseId,
            @RequestPart(value = "entreprise", required = false) String entrepriseJson,
            @RequestPart(value = "logo", required = false) MultipartFile imageLogoFile,
            HttpServletRequest request) {
    
        try {
            UpdateEntrepriseDTO dto = new UpdateEntrepriseDTO();
    
            if (entrepriseJson != null && !entrepriseJson.isBlank()) {
                dto = new ObjectMapper().readValue(entrepriseJson, UpdateEntrepriseDTO.class);
            }
    
            entrepriseService.updateEntreprise(entrepriseId, dto, imageLogoFile);
    
            return ResponseEntity.ok("Entreprise mise à jour avec succès !");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur : " + e.getMessage());
        }
    }
    





   
}
